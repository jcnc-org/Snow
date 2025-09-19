package org.jcnc.snow.vm.commands.system.control.fd;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.FDTable;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code Dup2Handler} 用于实现系统调用 DUP2。
 *
 * <p>
 * 功能：将 {@code oldfd} 复制到 {@code newfd}。
 * 如果 {@code newfd} 已经被打开，则先关闭它。
 * 新旧两个 fd 指向同一个底层通道（Channel）。
 * </p>
 *
 * <p>调用约定：</p>
 * <ul>
 *   <li>入参：{@code oldfd:int}, {@code newfd:int}</li>
 *   <li>出参：{@code newfd:int}</li>
 * </ul>
 *
 * <p>说明：</p>
 * <ul>
 *   <li>如果 {@code oldfd == newfd}，调用无效操作，直接返回 {@code newfd}。</li>
 *   <li>多个 fd 可以指向同一个底层通道。</li>
 *   <li>关闭 {@code newfd} 并不会影响 {@code oldfd}。</li>
 *   <li>如果 {@code oldfd} 无效，会抛出异常。</li>
 * </ul>
 *
 * <p>异常：</p>
 * <ul>
 *   <li>如果参数不是整数，抛出 {@link IllegalArgumentException}</li>
 *   <li>如果 {@code oldfd} 不存在，抛出 {@link IllegalArgumentException}</li>
 *   <li>I/O 操作失败时，抛出 {@link java.io.IOException}</li>
 * </ul>
 */
public class Dup2Handler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // 从操作数栈中依次弹出参数：newfd:int（栈顶），oldfd:int（栈底）
        Object newfdObj = stack.pop();
        Object oldfdObj = stack.pop();

        // 校验参数类型
        if (!(oldfdObj instanceof Number) || !(newfdObj instanceof Number)) {
            throw new IllegalArgumentException("DUP2: arguments must be int, got: "
                    + oldfdObj + ", " + newfdObj);
        }

        int oldfd = ((Number) oldfdObj).intValue();
        int newfd = ((Number) newfdObj).intValue();

        // 使用 FDTable 的 dup2 实现
        int resultFd = FDTable.dup2(oldfd, newfd);

        // 将结果压回操作数栈
        stack.push(resultFd);
    }
}
