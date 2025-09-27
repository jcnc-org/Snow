package org.jcnc.snow.vm.commands.system.control.sync;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.MutexRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code MutexNewHandler} 实现 MUTEX_NEW (0x1600) 系统调用，
 * 创建一个新的互斥量并返回其 mid。
 *
 * <p><b>Stack：</b>
 * 入参 {@code ()} →
 * 出参 {@code (mid:int)}
 * </p>
 *
 * <p><b>语义：</b>
 * 新建一个互斥量，分配唯一 mid 并登记到 {@link MutexRegistry}。
 * </p>
 *
 * <p><b>返回：</b>
 * 返回分配的互斥量 mid（int）。
 * </p>
 *
 * <p><b>异常：</b>
 * 正常情况下无异常，资源不足时抛出异常。
 * </p>
 */
public class MutexNewHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        int mid = MutexRegistry.create();
        stack.push(mid);
    }
}
