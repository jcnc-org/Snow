package org.jcnc.snow.vm.commands.system.control.sys;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code NcpuHandler} 实现 NCPU (0x1902) 系统调用，
 * 用于返回逻辑处理器（CPU）数量。
 *
 * <p><b>Stack</b>：入参 {@code ()} → 出参 {@code (n:int)}</p>
 *
 * <p><b>语义</b>：返回运行时可用的逻辑处理器数（等同于 {@code Runtime.getRuntime().availableProcessors()}）。</p>
 *
 * <p><b>返回</b>：成功返回一个非负整数 {@code n}。</p>
 *
 * <p><b>异常</b>：正常情况下不抛出异常；底层平台极端故障时可能抛出运行时异常。</p>
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
