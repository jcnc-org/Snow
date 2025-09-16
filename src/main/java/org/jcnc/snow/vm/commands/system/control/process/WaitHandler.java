package org.jcnc.snow.vm.commands.system.control.process;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.ProcessRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * WAIT(pid:int?) -> status:int
 *
 * 等待子进程结束。pid 为 null/0 表示任意子进程。
 */
public class WaitHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 入参：可能为 null
        Object pidObj = stack.pop();
        Integer pid = (pidObj instanceof Integer) ? (Integer) pidObj : null;

        int status = -1;

        if (pid == null || pid == 0) {
            // 2. 等待任意子进程
            for (Process p : ProcessRegistry.all()) {
                try {
                    status = p.waitFor();
                    ProcessRegistry.unregister(p.pid());
                    break;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } else {
            // 3. 等待指定 pid
            Process p = ProcessRegistry.get(pid.longValue());
            if (p == null) {
                throw new IllegalArgumentException("Invalid pid: " + pid);
            }
            try {
                status = p.waitFor();
            } finally {
                ProcessRegistry.unregister(pid.longValue());
            }
        }

        // 4. 压回退出码
        stack.push(status);
    }
}
