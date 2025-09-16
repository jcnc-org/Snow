package org.jcnc.snow.vm.commands.system.control.socket;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.SocketRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.channels.SocketChannel;

/**
 * {@code ShutdownHandler} 实现 SHUTDOWN (0x140E) 系统调用，
 * 用于关闭 TCP socket 的输入、输出或两端流。
 *
 * <p><b>Stack</b>：入参 {@code (fd:int, how:int)} → 出参 {@code (0:int)}</p>
 *
 * <p><b>语义</b>：根据 how 值，关闭 socket 的输入流、输出流，或同时关闭。</p>
 *
 * <p><b>返回</b>：始终返回 0。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>fd 无效时抛出 {@link IllegalArgumentException}</li>
 *   <li>how 参数非法时抛出 {@link IllegalArgumentException}</li>
 *   <li>关闭失败时抛出 {@link java.io.IOException}</li>
 * </ul>
 * </p>
 */
public class ShutdownHandler implements SyscallHandler {

    /**
     * 处理 SHUTDOWN 调用。
     *
     * @param stack     操作数栈，依次提供 fd、how
     * @param locals    局部变量存储器（未使用）
     * @param callStack 调用栈（未使用）
     * @throws Exception fd 无效、how 参数非法或关闭失败时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 参数顺序: how → fd
        int how = (int) stack.pop();
        int fd = (int) stack.pop();

        // 2. 获取 SocketChannel
        SocketChannel channel = (SocketChannel) SocketRegistry.get(fd);
        if (channel == null) {
            throw new IllegalArgumentException("Invalid socket fd: " + fd);
        }

        // 3. 执行 shutdown
        switch (how) {
            case 0: // SHUT_RD
                channel.socket().shutdownInput();
                break;
            case 1: // SHUT_WR
                channel.socket().shutdownOutput();
                break;
            case 2: // SHUT_RDWR
                channel.socket().shutdownInput();
                channel.socket().shutdownOutput();
                break;
            default:
                throw new IllegalArgumentException("Invalid how value: " + how);
        }

        // 4. 返回 0
        stack.push(0);
    }
}
