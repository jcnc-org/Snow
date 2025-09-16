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

public class GetSockNameHandler implements SyscallHandler {
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

        if (!(local instanceof InetSocketAddress)) {
            throw new IllegalStateException("Local address unavailable for fd=" + fd);
        }

        InetSocketAddress inet = (InetSocketAddress) local;
        String addr = inet.getAddress().getHostAddress();
        int port = inet.getPort();

        // 4. 压回 addr, port
        stack.push(addr);
        stack.push(port);
    }
}
