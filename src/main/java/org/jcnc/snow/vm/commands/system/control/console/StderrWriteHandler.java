package org.jcnc.snow.vm.commands.system.control.console;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code StderrWriteHandler} 是一个系统调用处理器，
 * 用于实现虚拟机中的标准错误输出功能。
 *
 * <p>该处理器会从操作数栈弹出一个对象，并将其字符串形式写入
 * {@link System#err} 流。</p>
 *
 * <p>操作完成后，向操作数栈压入返回值 {@code 0}，表示执行成功。</p>
 */
public class StderrWriteHandler implements SyscallHandler {

    /**
     * 处理写入标准错误输出操作（STDERR_WRITE）。
     *
     * <p>从栈顶弹出一个对象，将其转为字符串并写入标准错误流。
     * 写入完成后压入 {@code 0}。</p>
     *
     * @param stack     操作数栈，提供需要输出的数据并接收返回值
     * @param locals    局部变量存储器（本方法未使用）
     * @param callStack 调用栈（本方法未使用）
     * @throws Exception 执行过程中发生错误时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 从栈顶弹出待输出数据对象
        Object dataObj = stack.pop();

        // 写入标准错误流（直接使用 System.err）
        if (dataObj != null) {
            System.err.print(dataObj.toString());
        } else {
            System.err.print("null");
        }

        // 压入返回值 0 表示成功
        stack.push(0);
    }
}
