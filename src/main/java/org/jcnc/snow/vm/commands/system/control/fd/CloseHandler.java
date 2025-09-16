package org.jcnc.snow.vm.commands.system.control.fd;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.FDTable;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code CloseHandler} 用于实现系统调用 CLOSE。
 *
 * <p>
 * 功能：关闭指定的虚拟文件描述符（fd），并从 {@link FDTable} 中移除。
 * </p>
 *
 * <p>调用约定：</p>
 * <ul>
 *   <li>入参：{@code fd:int}</li>
 *   <li>出参：无</li>
 * </ul>
 *
 * <p>说明：</p>
 * <ul>
 *   <li>标准输入输出错误（fd=0,1,2）不会被关闭。</li>
 *   <li>关闭后再次使用同一 fd 会抛出错误。</li>
 * </ul>
 *
 * <p>异常：</p>
 * <ul>
 *   <li>如果 fd 不是整数，抛出 {@link IllegalArgumentException}</li>
 *   <li>I/O 操作失败时，抛出 {@link java.io.IOException}</li>
 * </ul>
 */
public class CloseHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // 从操作数栈中弹出参数：fd:int
        Object fdObj = stack.pop();

        // 校验参数类型
        if (!(fdObj instanceof Number)) {
            throw new IllegalArgumentException("CLOSE: fd must be an int, got: " + fdObj);
        }

        int fd = ((Number) fdObj).intValue();

        // 调用 FDTable 关闭对应通道
        FDTable.close(fd);

        // 向栈压入 0，保持栈平衡
        stack.push(0);    }
}
