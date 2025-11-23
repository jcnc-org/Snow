package org.jcnc.snow.vm.commands.system.control.console;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code StderrWriteHandler} 实现 STDERR_WRITE (0x1202) 系统调用，
 * 用于向标准错误输出（stderr）写入字符串。
 *
 * <p><b>Stack</b>：入参 {@code (data:Object)} → 出参 {@code (rc:int)}</p>
 *
 * <p><b>语义</b>：将对象转换为字符串（null 输出为 "null"），写入 {@code System.err}；
 * 操作完成后返回 {@code 0}。</p>
 *
 * <p><b>返回</b>：成功返回 {@code 0}。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>写入过程中发生 I/O 错误时抛出 {@link Exception}</li>
 * </ul>
 * </p>
 */
public class StderrWriteHandler implements SyscallHandler {

    /**
     * 处理系统调用 STDERR_WRITE 的具体实现。
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

        // 写入标准错误流（System.err），null 则输出 "null"
        if (dataObj != null) {
            System.err.print(dataObj.toString());
        } else {
            System.err.print("null");
        }
        // 确保立即刷新
        System.err.flush();

        // 向栈压入 0，保持栈平衡
        stack.push(0);
    }
}
