package org.jcnc.snow.vm.commands.system.control.socket;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.SocketRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * {@code GetSockNameHandler} 实现 GETSOCKNAME (0x1406) 系统调用，
 * 用于获取 socket 的本地地址和端口。
 *
 * <p><b>Stack</b>：入参 {@code (fd:int)} → 出参 {@code (addr:String, port:int)}</p>
 *
 * <p><b>语义</b>：返回 fd 对应 socket 的本地（绑定）IP 和端口。</p>
 *
 * <p><b>返回</b>：
 * <ul>
 *   <li>addr：本地 IP 地址（String）</li>
 *   <li>port：本地端口号（int）</li>
 * </ul>
 * </p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>fd 无效时抛出 {@link IllegalArgumentException}</li>
 *   <li>channel 类型不支持或本地地址不可用时抛出 {@link IllegalStateException}</li>
 * </ul>
 * </p>
 */
public class GetSockNameHandler implements SyscallHandler {

    /**
     * 处理 GETSOCKNAME 调用。
     *
     * @param stack     操作数栈，提供 socket fd，返回本地地址和端口
     * @param locals    局部变量存储器（未使用）
     * @param callStack 调用栈（未使用）
     * @throws Exception fd 无效、channel 类型不支持或本地地址不可用时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 从栈里取出 fd
        int fd = (int) stack.pop();

        // 2. 获取 Channel
        Channel ch = SocketRegistry.get(fd);
        if (ch == null) {
            throw new IllegalArgumentException("Invalid socket fd: " + fd);
        }

        // 3. 获取本地地址
        SocketAddress local;
        if (ch instanceof SocketChannel) {
            local = ((SocketChannel) ch).getLocalAddress();
        } else if (ch instanceof ServerSocketChannel) {
            local = ((ServerSocketChannel) ch).getLocalAddress();
        } else {
            throw new IllegalStateException("Unsupported channel type: " + ch.getClass());
        }

        if (!(local instanceof InetSocketAddress inet)) {
            throw new IllegalStateException("Local address unavailable for fd=" + fd);
        }

        String addr = inet.getAddress().getHostAddress();
        int port = inet.getPort();

        // 4. 压回 addr, port
        stack.push(addr);
        stack.push(port);
    }
}
