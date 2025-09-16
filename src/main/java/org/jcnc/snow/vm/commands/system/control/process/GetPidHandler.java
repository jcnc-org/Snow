package org.jcnc.snow.vm.commands.system.control.process;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * GETPID() -> pid:int
 *
 * 返回当前进程 ID。
 */
public class GetPidHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 获取当前 JVM 进程 ID (JDK 9+)
        long pid = ProcessHandle.current().pid();

        // 2. 压回结果 (取 int 即可，如果你需要兼容大 PID，可以保留 long)
        stack.push((int) pid);
    }
}
