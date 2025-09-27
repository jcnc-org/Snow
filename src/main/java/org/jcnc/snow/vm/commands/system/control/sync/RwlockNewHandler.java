package org.jcnc.snow.vm.commands.system.control.sync;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.RwlockRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code RwlockNewHandler} 实现 RWLOCK_NEW (0x160B) 系统调用，
 * 创建一个新的读写锁并返回其 ID。
 *
 * <p><b>Stack：</b>
 * 入参 {@code ()} →
 * 出参 {@code (rwl:int)}
 * </p>
 *
 * <p><b>语义：</b>
 * 新建一个读写锁，分配唯一 ID 并登记到 {@link RwlockRegistry}。
 * </p>
 *
 * <p><b>返回：</b>
 * 返回分配的读写锁 ID（int）。
 * </p>
 *
 * <p><b>异常：</b>
 * 正常情况下无异常。
 * </p>
 */
public class RwlockNewHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        int id = RwlockRegistry.create();
        stack.push(id);
    }
}
