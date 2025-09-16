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
        // 使用单调时钟（System.nanoTime）转换为毫秒，避免受系统时间调整影响
        long ms = System.nanoTime() / 1_000_000L;
        stack.push(ms);
    }
}
