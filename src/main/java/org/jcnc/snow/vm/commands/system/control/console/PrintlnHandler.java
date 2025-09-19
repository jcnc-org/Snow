package org.jcnc.snow.vm.commands.system.control.console;

import org.jcnc.snow.vm.commands.system.control.SyscallUtils;
import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code PrintlnHandler} 用于实现系统调用 PRINTLN。
 *
 * <p>
 * 功能：将操作数栈顶的对象输出到标准输出流（控制台），并自动换行。
 *      具体输出逻辑通过 {@link SyscallUtils#output(Object, boolean)} 实现。
 * </p>
 *
 * <p>调用约定：</p>
 * <ul>
 *   <li>入参：{@code data:Object}，待输出的数据对象</li>
 *   <li>出参：{@code 0:int}，表示操作成功</li>
 * </ul>
 *
 * <p>说明：</p>
 * <ul>
 *   <li>输出操作完成后，始终向操作数栈压入 0 以保持栈平衡。</li>
 *   <li>本操作不会抛出异常，所有异常将由上层捕获处理。</li>
 * </ul>
 *
 * <p>异常：</p>
 * <ul>
 *   <li>执行过程中发生错误时，抛出 {@link Exception}</li>
 * </ul>
 */
public class PrintlnHandler implements SyscallHandler {

    /**
     * 处理系统调用 PRINTLN 的具体实现。
     *
     * @param stack     操作数栈，提供输出数据并接收返回值
     * @param locals    局部变量存储器（本方法未使用）
     * @param callStack 调用栈（本方法未使用）
     * @throws Exception 执行过程中发生错误时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 从操作数栈弹出待输出的数据对象
        Object dataObj = stack.pop();

        // 调用工具方法输出数据到标准输出，并换行
        SyscallUtils.output(dataObj, true);

        // 向栈压入 0，保持栈平衡
        stack.push(0);
    }
}
