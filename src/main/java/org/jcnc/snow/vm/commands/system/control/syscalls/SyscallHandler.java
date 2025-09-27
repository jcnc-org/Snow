package org.jcnc.snow.vm.commands.system.control.syscalls;

import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

public interface SyscallHandler {
    void handle(OperandStack stack,
                LocalVariableStore locals,
                CallStack callStack) throws Exception;
}
