package org.jcnc.snow.vm.commands.system.control.socket;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.SocketRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

/**
 * {@code GetPeerNameHandler} 实现 GETPEERNAME (0x140C) 系统调用，
 * 用于获取 socket 对端（远端）的地址和端口。
 *
 * <p><b>Stack：</b>
 * 入参 {@code (fd:int)} →
 * 出参 {@code (tuple:any[])}
 * </p>
 *
 * <p><b>语义：</b>
 * 返回与 fd 对应的 SocketChannel 的远端 IP 和端口。
 * </p>
 *
 * <p><b>返回：</b>
 * 返回 {addr, port} 数组：
 * <ul>
 *   <li>[0] = addr:String</li>
 *   <li>[1] = port:int</li>
 * </ul>
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>fd 无效时抛出 {@link IllegalArgumentException}</li>
 *   <li>socket 未连接或获取不到对端地址时抛出 {@link IllegalStateException}</li>
 * </ul>
 * </p>
 */
public class GetPeerNameHandler implements SyscallHandler {

    /**
     * 处理 GETPEERNAME 调用。
     *
     * @param stack     操作数栈，提供 socket fd，返回对端地址和端口
     * @param locals    局部变量存储器（未使用）
     * @param callStack 调用栈（未使用）
     * @throws Exception fd 无效或 socket 未连接时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 取出 fd
        int fd = (int) stack.pop();

        // 2. 获取 SocketChannel
        SocketChannel channel = (SocketChannel) SocketRegistry.get(fd);
        if (channel == null) {
            throw new IllegalArgumentException("Invalid socket fd: " + fd);
        }

        // 3. 获取对端地址
        SocketAddress remote = channel.getRemoteAddress();
        if (!(remote instanceof InetSocketAddress inet)) {
            throw new IllegalStateException("Socket is not connected or remote address unavailable");
        }

        String addr = inet.getAddress().getHostAddress();
        int port = inet.getPort();

        // 4. 返回数组 {addr, port}
        Object[] result = new Object[]{addr, port};
        stack.push(result);
    }
}
