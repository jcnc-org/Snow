package org.jcnc.snow.vm.commands.system.control.sys;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * 实现 NCPU () -> n:int
 * <p>
 * 将逻辑处理器数量（Runtime.availableProcessors()）作为 Integer 推回操作数栈。
 */
public class NcpuHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        int ncpu = Runtime.getRuntime().availableProcessors();
        stack.push(ncpu);
    }
}
