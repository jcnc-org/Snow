package org.jcnc.snow.vm.commands.system.control.socket;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.SocketRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * {@code AcceptHandler} 实现 ACCEPT (0x1403) 系统调用，
 * 用于接受一个传入的 socket 连接。
 *
 * <p><b>Stack</b>：入参 {@code (fd:int)} → 出参 {@code (cfd:int, addr:String, port:int)}</p>
 *
 * <p><b>语义</b>：阻塞等待并接受传入连接，返回新连接 fd、对端地址和端口。</p>
 *
 * <p><b>返回</b>：
 * <ul>
 *   <li>cfd：新连接的 socket fd</li>
 *   <li>addr：对端 IP 地址（String）</li>
 *   <li>port：对端端口号（int）</li>
 * </ul>
 * </p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>fd 无效时抛出 {@link IllegalArgumentException}</li>
 *   <li>accept 失败或返回 null 时抛出 {@link RuntimeException}</li>
 * </ul>
 * </p>
 */
public class AcceptHandler implements SyscallHandler {

    /**
     * 处理 ACCEPT 调用。
     *
     * @param stack     操作数栈，提供 server fd，并返回新连接 fd、地址、端口
     * @param locals    局部变量存储器（未使用）
     * @param callStack 调用栈（未使用）
     * @throws Exception fd 无效或 accept 失败时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 从操作数栈取出 fd
        int fd = (int) stack.pop();

        // 2. 查找对应的 ServerSocketChannel
        ServerSocketChannel server = (ServerSocketChannel) SocketRegistry.get(fd);
        if (server == null) {
            throw new IllegalArgumentException("Invalid socket fd: " + fd);
        }

        // 3. 执行 accept（阻塞等待）
        SocketChannel client = server.accept();
        if (client == null) {
            throw new RuntimeException("Accept returned null (non-blocking mode?)");
        }

        // 4. 为 client 分配新的 fd
        int cfd = SocketRegistry.register(client);

        // 5. 获取对端地址
        SocketAddress remote = client.getRemoteAddress();
        String addr = "";
        int port = 0;
        if (remote instanceof InetSocketAddress) {
            InetSocketAddress inet = (InetSocketAddress) remote;
            addr = inet.getAddress().getHostAddress();
            port = inet.getPort();
        }

        // 6. 按顺序压栈: cfd, addr, port
        stack.push(cfd);
        stack.push(addr);
        stack.push(port);
    }
}
