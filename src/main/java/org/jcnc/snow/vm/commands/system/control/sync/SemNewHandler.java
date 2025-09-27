package org.jcnc.snow.vm.commands.system.control.sync;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.SemRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code SemNewHandler} 实现 SEM_NEW (0x1608) 系统调用，
 * 用于创建一个新的信号量，并指定初始值。
 *
 * <p><b>Stack：</b>
 * 入参 {@code (init:int)} →
 * 出参 {@code (sid:int)}
 * </p>
 *
 * <p><b>语义：</b>
 * 新建一个初始值为 {@code init} 的信号量，分配唯一 sid 并登记到 {@link SemRegistry}。
 * </p>
 *
 * <p><b>返回：</b>
 * 成功返回新分配的信号量 ID（sid:int）。
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>init 非 int 时抛出 {@link IllegalArgumentException}</li>
 *   <li>资源不足或内部错误时抛出异常</li>
 * </ul>
 * </p>
 */
public class SemNewHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        Object initObj = stack.pop();
        if (!(initObj instanceof Integer)) {
            throw new IllegalArgumentException("SEM_NEW: init must be int");
        }
        int sid = SemRegistry.create((Integer) initObj);
        stack.push(sid);
    }
}
