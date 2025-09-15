package org.jcnc.snow.vm.commands.system.control.fs;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

public class UtimeHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // TODO: 实现 UTIME (path:string, mtime:long, atime:long) -> 0
    }
}
