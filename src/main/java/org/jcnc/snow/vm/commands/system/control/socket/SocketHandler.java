package org.jcnc.snow.vm.commands.system.control.socket;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.SocketRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class SocketHandler implements SyscallHandler {
    // 常量定义 (可在全局常量表里维护)
    private static final int AF_INET = 2;
    private static final int AF_INET6 = 10;

    private static final int SOCK_STREAM = 1;
    private static final int SOCK_DGRAM  = 2;

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 参数顺序: proto → type → family
        int proto = (int) stack.pop();
        int type = (int) stack.pop();
        int family = (int) stack.pop();

        // 2. 检查 family
        if (family != AF_INET && family != AF_INET6) {
            throw new UnsupportedOperationException("Unsupported family: " + family);
        }

        int fd;

        // 3. 根据 type 创建不同的 Channel
        if (type == SOCK_STREAM) {
            // TCP：既可能是客户端 socket，也可能是服务端 socket
            // 这里默认创建 ServerSocketChannel 供 bind/listen/accept 使用
            ServerSocketChannel server = ServerSocketChannel.open();
            server.configureBlocking(true);
            fd = SocketRegistry.register(server);
        } else if (type == SOCK_DGRAM) {
            // UDP
            DatagramChannel udp = DatagramChannel.open();
            udp.configureBlocking(true);
            fd = SocketRegistry.register(udp);
        } else {
            throw new UnsupportedOperationException("Unsupported socket type: " + type);
        }

        // 4. 压回 fd
        stack.push(fd);
    }
}
