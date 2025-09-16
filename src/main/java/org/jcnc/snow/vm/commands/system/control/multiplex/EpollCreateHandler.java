package org.jcnc.snow.vm.commands.system.control.multiplex;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.EpollRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code EpollCreateHandler} 实现 EPOLL_CREATE (0x1301)  系统调用，
 * 用于在虚拟机中创建一个新的 epoll 实例。
 *
 * <p><b>Stack</b>：入参 {@code (flags:int?)} → 出参 {@code (epfd:int)}</p>
 *
 * <p><b>语义</b>：创建一个新的 epoll 文件描述符，并在内部 {@link EpollRegistry} 注册，
 * 返回对应的 epfd（正整数）。</p>
 *
 * <p><b>返回</b>：成功时返回新的 epfd。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>资源不足时抛出 {@link OutOfMemoryError} 或 {@link IllegalStateException}</li>
 *   <li>flags 非法时抛出 {@link IllegalArgumentException}</li>
 *   <li>底层 I/O 错误时抛出 {@link java.io.IOException}</li>
 * </ul>
 * </p>
 */
public class EpollCreateHandler implements SyscallHandler {

    /**
     * 处理 EPOLL_CREATE 调用。
     *
     * @param stack     操作数栈，提供可选 flags 并接收返回的 epfd
     * @param locals    局部变量存储器（未使用）
     * @param callStack 调用栈（未使用）
     * @throws Exception 当创建 epoll 失败时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 判断是否传入 flags 参数
        int flags;
        if (!stack.isEmpty() && stack.peek() instanceof Integer) {
            flags = (int) stack.pop();
        } else {
            flags = 0; // 默认值
        }

        // 创建 epoll，并返回 epfd
        int epfd = EpollRegistry.create(flags);

        // 将结果压入栈顶
        stack.push(epfd);
    }
}
