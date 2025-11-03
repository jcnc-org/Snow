package org.jcnc.snow.vm.commands.system.control.process;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.EnvRegistry;
import org.jcnc.snow.vm.io.ProcessRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code ForkHandler} 实现 FORK (0x1501) 系统调用，
 * 启动外部子进程并同步等待其结束。
 *
 * <p><b>Stack：</b>
 * 入参 {@code (cmd:List<String>)} → 出参 {@code (pid:int)}
 * </p>
 *
 * <p><b>语义：</b>
 * <ul>
 *   <li>以给定命令参数 {@code cmd} 启动新子进程，{@code cmd[0]} 通常为程序路径</li>
 *   <li>新进程继承当前虚拟机的环境变量快照（{@link EnvRegistry#snapshot()}）</li>
 *   <li>标准输入继承父进程（{@code redirectInput(INHERIT)}）</li>
 *   <li>标准输出与标准错误通过后台线程实时转发到当前控制台</li>
 *   <li>调用线程阻塞，直到子进程执行完毕后再继续执行</li>
 *   <li>子进程注册到 {@link ProcessRegistry}，供后续 wait/kill 等系统调用查找</li>
 * </ul>
 * </p>
 *
 * <p><b>返回：</b>
 * <ul>
 *   <li>成功时返回子进程 PID（int），如无法获取则返回 {@code 0}</li>
 *   <li>启动失败时返回 {@code -1} 并抛出异常</li>
 * </ul>
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>参数类型不符时抛出 {@link IllegalArgumentException}</li>
 *   <li>命令数组元素不是 String 时抛出 {@link IllegalArgumentException}</li>
 *   <li>I/O 错误（进程启动失败）时抛出 {@link IOException}</li>
 * </ul>
 * </p>
 *
 * <p><b>注意事项：</b>
 * <ul>
 *   <li>本实现为同步版本；调用线程会等待子进程执行结束</li>
 *   <li>由于阻塞等待，父进程不会在子进程结束前退出</li>
 * </ul>
 * </p>
 */

public class ForkHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 取参数（必须为字符串 List）
        Object cmdObj = stack.pop();
        if (!(cmdObj instanceof List<?> list)) {
            throw new IllegalArgumentException("FORK: 参数必须是字符串数组");
        }

        List<String> cmd = new ArrayList<>(list.size());
        for (Object o : list) {
            if (!(o instanceof String)) {
                throw new IllegalArgumentException(
                        "FORK: 命令数组必须全部是 string，得到: " + o
                );
            }
            cmd.add((String) o);
        }

        try {
            // 2. 构造进程
            ProcessBuilder pb = new ProcessBuilder(cmd);

            // 3. 合并环境
            pb.environment().putAll(EnvRegistry.snapshot());

            // 4. 子进程 stdin 继承父进程
            pb.redirectInput(ProcessBuilder.Redirect.INHERIT);

            // stdout/stderr 保持默认 (PIPE)，我们手动转发

            // 5. 启动子进程
            Process child = pb.start();

            // 6. 注册进程（让 wait/kill 仍然有机会找到它）
            ProcessRegistry.register(child);

            // 7. 启动输出转发线程（把 stdout/stderr 实时打印到本进程控制台）
            Thread outFwd = new StreamForwarder(
                    child.getInputStream(),
                    System.out,
                    "snow-exec-stdout-" + cmd
            );
            outFwd.start();

            Thread errFwd = new StreamForwarder(
                    child.getErrorStream(),
                    System.err,
                    "snow-exec-stderr-" + cmd
            );
            errFwd.start();

            // 8. 阻塞等待子进程完成
            child.waitFor();

            // 等转发线程把尾巴读完再收工。
            // 这里 join() 可以避免最后一行被截断。
            try {
                outFwd.join();
            } catch (InterruptedException ignored) {
            }
            try {
                errFwd.join();
            } catch (InterruptedException ignored) {
            }

            // 9. 获取 PID
            int pidToPush;
            try {
                long pid = child.pid();
                pidToPush = (int) pid;
            } catch (UnsupportedOperationException e) {
                pidToPush = 0; // 拿不到 pid 但进程确实跑完了
            }

            // 10. 压栈返回
            stack.push(pidToPush);

        } catch (IOException e) {
            // 11. 启动失败
            stack.push(-1);
            throw e;
        }
    }

    /**
     * 把子进程流同步拷贝到父进程输出。
     * 注意：这是阻塞拷贝方法，不是线程。
     * 我们会在单独线程里跑它。
     */
    private static final class StreamForwarder extends Thread {
        private final InputStream src;
        private final PrintStream dst;

        StreamForwarder(InputStream src, PrintStream dst, String threadName) {
            super(threadName);
            this.src = src;
            this.dst = dst;
            // 这里我们可以把它设成 daemon，因为主线程会 waitFor() 子进程，
            // 也就是说主线程不会立刻 System.exit() 了，daemon 不会被提前杀。
            setDaemon(true);
        }

        @Override
        public void run() {
            byte[] buf = new byte[1024];
            try (InputStream in = src) {
                int n;
                while ((n = in.read(buf)) != -1) {
                    dst.write(buf, 0, n);
                    dst.flush();
                }
            } catch (IOException ignored) {
                // 子进程结束/流关闭 -> 正常
            }
        }
    }
}
