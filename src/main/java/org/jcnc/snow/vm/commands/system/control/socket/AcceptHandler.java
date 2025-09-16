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

public class AcceptHandler implements SyscallHandler {
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
