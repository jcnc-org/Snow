package org.jcnc.snow.vm.commands.system.control.console;

import org.jcnc.snow.vm.commands.system.control.SyscallUtils;
import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * <p>
 * {@code PrintHandler} 是一个系统调用处理器，实现虚拟机中的标准输出功能（类似于控制台打印）。
 * 该处理器会从操作数栈弹出一个对象，并通过 {@link SyscallUtils#output(Object, boolean)} 方法打印到输出流（通常为标准输出）。
 * </p>
 *
 * <p>
 * 打印操作完成后，向操作数栈压入 0 作为返回值，表示操作成功。
 * </p>
 *
 * <p>
 * 使用场景示例：用于脚本或虚拟机指令中输出文本或变量值。
 * </p>
 */
public class PrintHandler implements SyscallHandler {

    /**
     * 处理打印输出操作（PRINT）。
     * <p>
     * 从操作数栈弹出一个对象，调用 {@link SyscallUtils#output(Object, boolean)} 方法进行输出，
     * 并在操作完成后压入返回值 0。
     * </p>
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
        // 调用工具方法输出数据
        SyscallUtils.output(dataObj, false);
        // 压入返回值 0
        stack.push(0);
    }
}
