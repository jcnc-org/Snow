package org.jcnc.snow.vm.commands.system.control.time;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

public class TickMsHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // TODO: 实现 TICK_MS () -> ms:long
    }
}
