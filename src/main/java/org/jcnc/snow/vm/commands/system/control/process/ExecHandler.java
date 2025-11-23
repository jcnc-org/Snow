package org.jcnc.snow.vm.commands.system.control.process;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;
import org.jcnc.snow.vm.io.EnvRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * {@code ExecHandler} 实现 EXEC (0x1502) 系统调用，
 * 启动新进程并用其输出“接管”当前控制台，随后终结当前 VM 进程。
 *
 * <p><b>Stack：</b>
 * 入参 {@code (env:Map<String,String>, argv:List<String>, path:String)} → 无返回
 * </p>
 *
 * <p><b>语义：</b>
 * <ul>
 *   <li>以给定可执行文件 {@code path} 及参数 {@code argv} 启动一个新进程</li>
 *   <li>新进程环境变量 = 虚拟机当前的 {@link EnvRegistry#snapshot()}，再叠加 {@code env}</li>
 *   <li>新进程的标准输出/错误输出会被实时转发到当前控制台</li>
 *   <li>当前线程会阻塞等待该进程结束，然后当前 VM 会直接退出（不返回 Snow 代码）</li>
 * </ul>
 * </p>
 *
 * <p><b>返回：</b>
 * <ul>
 *   <li>无返回值：本系统调用不会返回到调用方。目标进程结束后，当前 VM 直接终止</li>
 * </ul>
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>path 不是 String 时抛出 {@link IllegalArgumentException}</li>
 *   <li>进程启动失败时抛出 {@link IOException}</li>
 *   <li>栈参数不足时抛出 {@link IllegalStateException}</li>
 * </ul>
 * </p>
 *
 * <p><b>注意事项：</b>
 * <ul>
 *   <li>这是“接管式”执行：Snow 进程最终会终止，不会回到调用点继续运行</li>
 *   <li>与传统 Unix {@code execve()} 类似，但在实际实现层面我们会等待子进程退出后再终止自身，
 *       以确保在 GraalVM native-image 下也能正确刷新/显示子进程输出</li>
 * </ul>
 * </p>
 */
public class ExecHandler implements SyscallHandler {

    /**
     * 将子进程的一个输出流（stdout 或 stderr）持续复制到给定的 PrintStream。
     * 我们用线程异步转发，保证即使在 native-image 下也能看到子进程的输出。
     */
    private static final class StreamForwarder extends Thread {
        private final InputStream src;
        private final PrintStream dst;

        StreamForwarder(InputStream src, PrintStream dst, String name) {
            super(name);
            this.src = src;
            this.dst = dst;
            // 它只是帮忙转发输出，不决定进程生命周期，设为 daemon 即可
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
                // 子进程结束 / 管道关闭 是正常情况
            }
        }
    }

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 参数检查
        if (stack.size() < 3) {
            throw new IllegalStateException("EXEC 需要 3 个参数 (env:Map, argv:List, path:String)");
        }

        // 2. 取参（注意顺序：Snow 调用顺序是 (env, argv, path)，压栈后 path 在栈顶）
        Object pathObj = stack.pop(); // path
        Object argvObj = stack.pop(); // argv
        Object envObj  = stack.pop(); // env

        // 3. 检查 path 类型
        if (!(pathObj instanceof String path)) {
            throw new IllegalArgumentException("EXEC: path 必须是 String");
        }

        // 4. 参数类型处理
        @SuppressWarnings("unchecked")
        List<String> argv = (argvObj instanceof List)
                ? (List<String>) argvObj
                : List.of();

        @SuppressWarnings("unchecked")
        Map<String, String> env = (envObj instanceof Map)
                ? (Map<String, String>) envObj
                : Map.of();

        // 5. 组装命令行: [path, ...argv]
        List<String> command = new ArrayList<>(argv.size() + 1);
        command.add(path);
        command.addAll(argv);

        ProcessBuilder pb = new ProcessBuilder(command);

        // 6. 合并环境变量 (当前 VM 的 EnvRegistry.snapshot() 再叠加 env)
        Map<String, String> pbEnv = pb.environment();
        pbEnv.putAll(EnvRegistry.snapshot());
        if (!env.isEmpty()) {
            pbEnv.putAll(env);
        }

        // 标准输入可以继承：允许交互型命令继续读键盘
        pb.redirectInput(ProcessBuilder.Redirect.INHERIT);

        // 7. 启动子进程
        Process child = pb.start();

        // 8. 启动两个转发线程，把子进程输出实时打印到当前控制台
        Thread outForwarder = new StreamForwarder(
                child.getInputStream(),
                System.out,
                "snow-exec-stdout-" + command
        );
        outForwarder.start();

        Thread errForwarder = new StreamForwarder(
                child.getErrorStream(),
                System.err,
                "snow-exec-stderr-" + command
        );
        errForwarder.start();

        // 9. 等待子进程执行完毕
        child.waitFor();

        // 10. 尽量等转发线程吃完最后一口输出（短 join，不处理 interrupt）
        try {
            outForwarder.join(100);
        } catch (InterruptedException ignored) { }
        try {
            errForwarder.join(100);
        } catch (InterruptedException ignored) { }

        // 11. 终止当前 VM（不会返回到 Snow 代码）
        Runtime.getRuntime().halt(0);
    }
}
