package org.jcnc.snow.vm.commands.system.control.multiplex;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.EpollRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code EpollCreateHandler} 实现系统调用 <b>EPOLL_CREATE (0x1301)</b>，
 * 用于创建新的 epoll 实例，并返回 epoll 文件描述符（epfd）。
 *
 * <p>栈约定：</p>
 * <ul>
 *   <li>参数：flags（int，可选）</li>
 *   <li>返回：epfd（int，分配的 epoll 文件描述符）</li>
 * </ul>
 *
 * <p>语义：</p>
 * <ul>
 *   <li>在 {@link EpollRegistry} 中注册一个新的 epoll 实例</li>
 *   <li>flags 参数当前可选，默认值为 0</li>
 *   <li>成功时返回分配的 epfd</li>
 * </ul>
 */
public class EpollCreateHandler implements SyscallHandler {

    /**
     * 处理 EPOLL_CREATE 系统调用。
     *
     * @param stack     操作数栈，提供参数并返回结果
     * @param locals    局部变量表（未使用）
     * @param callStack 调用栈（未使用）
     * @throws Exception 处理失败时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 取出 flags 参数（可选，默认 0）
        int flags;
        if (!stack.isEmpty() && stack.peek() instanceof Integer) {
            flags = (int) stack.pop();
        } else {
            flags = 0;
        }

        // 2. 注册新 epoll 实例，获取 epfd
        int epfd = EpollRegistry.create(flags);

        // 3. 压回 epfd 作为 syscall 返回值
        stack.push(epfd);
    }
}
