package org.jcnc.snow.vm.commands.system.control.fd;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.FDTable;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code CloseHandler} 实现 CLOSE (0x1004) 系统调用，
 * 用于关闭指定的 fd。
 *
 * <p><b>Stack</b>：入参 {@code (fd:int)} → 出参：无</p>
 *
 * <p><b>语义</b>：关闭并从 {@code FDTable} 移除，释放底层通道。</p>
 *
 * <p><b>返回</b>：无。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>fd 非法时抛出 {@link IllegalArgumentException}</li>
 *   <li>I/O 操作失败时抛出 {@link java.io.IOException}</li>
 * </ul>
 * </p>
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
        stack.push(0);
    }
}
