package org.jcnc.snow.vm.commands.system.control.sync;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.CondRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code CondNewHandler} 实现 COND_NEW (0x1604) 系统调用，
 * 创建一个新的条件变量并返回其 cid。
 *
 * <p><b>Stack：</b>
 * 入参 {@code ()} →
 * 出参 {@code (cid:int)}
 * </p>
 *
 * <p><b>语义：</b>
 * 新建一个条件变量（monitor），分配唯一 cid 并登记到 {@link CondRegistry}。
 * </p>
 *
 * <p><b>返回：</b>
 * 返回分配的条件变量 cid（int）。
 * </p>
 *
 * <p><b>异常：</b>
 * 正常情况下无异常。
 * </p>
 */
public class CondNewHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        int cid = CondRegistry.create();
        stack.push(cid);
    }
}
