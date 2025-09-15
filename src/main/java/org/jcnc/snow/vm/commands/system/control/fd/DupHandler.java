package org.jcnc.snow.vm.commands.system.control.fd;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.FDTable;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code DupHandler} 用于实现系统调用 DUP。
 *
 * <p>
 * 功能：复制一个已打开的虚拟文件描述符（fd），返回新的 fd。
 * 新 fd 与旧 fd 指向同一个底层通道（Channel）。
 * </p>
 *
 * <p>调用约定：</p>
 * <ul>
 *   <li>入参：{@code oldfd:int}</li>
 *   <li>出参：{@code newfd:int}</li>
 * </ul>
 *
 * <p>说明：</p>
 * <ul>
 *   <li>多个 fd 可以指向同一个底层通道。</li>
 *   <li>关闭其中一个 fd 并不会影响另一个 fd 对应的通道。</li>
 *   <li>如果 oldfd 无效，会抛出异常。</li>
 * </ul>
 *
 * <p>异常：</p>
 * <ul>
 *   <li>如果参数不是整数，抛出 {@link IllegalArgumentException}</li>
 *   <li>如果 oldfd 不存在，抛出 {@link IllegalArgumentException}</li>
 * </ul>
 */
public class DupHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // 从操作数栈中弹出参数：oldfd:int
        Object fdObj = stack.pop();

        // 校验参数类型
        if (!(fdObj instanceof Number)) {
            throw new IllegalArgumentException("DUP: fd must be an int, got: " + fdObj);
        }

        int oldfd = ((Number) fdObj).intValue();

        // 调用 FDTable 执行 dup
        int newfd = FDTable.dup(oldfd);

        // 将新 fd 压回栈
        stack.push(newfd);
    }
}
