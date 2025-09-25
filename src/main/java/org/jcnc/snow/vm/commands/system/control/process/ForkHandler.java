package org.jcnc.snow.vm.commands.system.control.process;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code ForkHandler} 实现 FORK (0x1501) 系统调用，
 * 支持传入命令数组来启动子进程。
 *
 * <p><b>Stack</b>：入参 {@code (cmd:any[])} → 出参 {@code (pid:int)}</p>
 *
 * <p><b>语义</b>：通过 {@link ProcessBuilder} 启动一个子进程，
 * 父进程返回子进程 pid。命令行参数由 {@code cmd} 数组提供。</p>
 *
 * <p><b>返回</b>：成功时返回子进程 pid；失败时返回 -1。</p>
 */
public class ForkHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        Object cmdObj = stack.pop();
        if (!(cmdObj instanceof List<?> list)) {
            throw new IllegalArgumentException("FORK: 参数必须是字符串数组");
        }

        List<String> cmd = new ArrayList<>();
        for (Object o : list) {
            if (!(o instanceof String)) {
                throw new IllegalArgumentException("FORK: 命令数组必须全部是 string，得到: " + o);
            }
            cmd.add((String) o);
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.inheritIO(); // 继承父进程 IO
            Process child = pb.start();

            long pid;
            try {
                pid = child.pid();
            } catch (UnsupportedOperationException e) {
                pid = -1;
            }

            stack.push((int) pid);

        } catch (IOException e) {
            stack.push(-1);
            throw e;
        }
    }
}
