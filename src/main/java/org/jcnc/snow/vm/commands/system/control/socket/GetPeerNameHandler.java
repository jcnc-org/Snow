package org.jcnc.snow.vm.commands.system.control.socket;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.SocketRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

public class GetPeerNameHandler implements SyscallHandler {
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
        if (!(remote instanceof InetSocketAddress)) {
            throw new IllegalStateException("Socket is not connected or remote address unavailable");
        }

        InetSocketAddress inet = (InetSocketAddress) remote;
        String addr = inet.getAddress().getHostAddress();
        int port = inet.getPort();

        // 4. 压回 addr, port
        stack.push(addr);
        stack.push(port);
    }
}
