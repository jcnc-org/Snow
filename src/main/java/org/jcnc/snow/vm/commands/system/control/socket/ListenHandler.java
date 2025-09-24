package org.jcnc.snow.vm.commands.system.control.socket;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.SocketRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.channels.ServerSocketChannel;

/**
 * {@code ListenHandler} 实现 LISTEN (0x1402) 系统调用，
 * 用于将 ServerSocketChannel 设置为监听状态（实际 Java bind 时已监听）。
 *
 * <p><b>Stack</b>：入参 {@code (fd:int, backlog:int)} → 出参 {@code (0:int)}</p>
 *
 * <p><b>语义</b>：对 fd 指定的 ServerSocketChannel 设置监听（Java 下通常已自动监听）。</p>
 *
 * <p><b>返回</b>：始终返回 0。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>fd 无效时抛出 {@link IllegalArgumentException}</li>
 * </ul>
 * </p>
 */
public class ListenHandler implements SyscallHandler {

    /**
     * 处理 LISTEN 调用。
     *
     * @param stack     操作数栈，依次提供 backlog、fd
     * @param locals    局部变量存储器（未使用）
     * @param callStack 调用栈（未使用）
     * @throws Exception fd 无效时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 从栈中取参数 (顺序: backlog → fd)
        int backlog = (int) stack.pop();
        int fd = (int) stack.pop();

        // 2. 获取 ServerSocketChannel
        ServerSocketChannel server = (ServerSocketChannel) SocketRegistry.get(fd);
        if (server == null) {
            throw new IllegalArgumentException("Invalid socket fd: " + fd);
        }

        // 3. 在 Java 中，bind() 时就已经进入监听状态。
        // backlog 只能在 bind() 时指定，这里通常无法再修改。直接忽略 backlog，只返回成功。

        // 4. 返回 0
        stack.push(0);
    }
}
