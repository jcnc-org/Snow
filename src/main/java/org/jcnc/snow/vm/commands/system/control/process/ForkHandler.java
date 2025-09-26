package org.jcnc.snow.vm.commands.system.control.process;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.ProcessRegistry;
import org.jcnc.snow.vm.io.EnvRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code ForkHandler} 实现 FORK (0x1501) 系统调用，
 * 用于启动新子进程，命令参数可变。
 *
 * <p><b>Stack：</b>
 * 入参 {@code (cmd:List<String>)} → 出参 {@code (pid:int)}
 * </p>
 *
 * <p><b>语义：</b>
 * <ul>
 *   <li>以给定命令参数 {@code cmd} 启动子进程，cmd[0] 通常为程序路径</li>
 *   <li>新进程继承当前虚拟机的 setenv 环境变量快照（{@link EnvRegistry#snapshot()}）</li>
 *   <li>标准输入/输出/错误继承父进程（IO 透明）</li>
 *   <li>新进程注册到 {@link ProcessRegistry}，供后续 wait 系统调用查找</li>
 * </ul>
 * </p>
 *
 * <p><b>返回：</b>
 * <ul>
 *   <li>子进程 PID（int）。如无法获取则返回 {@code -1}</li>
 *   <li>如进程启动失败（抛异常），仍 push {@code -1} 到栈顶再抛出异常</li>
 * </ul>
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>参数类型不符时抛出 {@link IllegalArgumentException}</li>
 *   <li>命令数组元素不是 String 时抛出 {@link IllegalArgumentException}</li>
 *   <li>I/O 错误时抛出 {@link IOException}</li>
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
                throw new IllegalArgumentException("FORK: 命令数组必须全部是 string，得到: " + o);
            }
            cmd.add((String) o);
        }

        try {
            // 2. 构造进程
            ProcessBuilder pb = new ProcessBuilder(cmd);

            // 3. 注入 VM 环境
            pb.environment().putAll(EnvRegistry.snapshot());
            pb.inheritIO();

            Process child = pb.start();

            // 4. 注册进程
            ProcessRegistry.register(child);

            // 5. 获取 PID
            long pid;
            try {
                pid = child.pid();
            } catch (UnsupportedOperationException e) {
                pid = -1;
            }

            stack.push((int) pid);

        } catch (IOException e) {
            // 6. 进程启动失败，返回 -1 并抛出异常
            stack.push(-1);
            throw e;
        }
    }
}
