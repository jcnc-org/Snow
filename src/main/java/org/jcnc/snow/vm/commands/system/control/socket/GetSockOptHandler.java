package org.jcnc.snow.vm.commands.system.control.socket;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.SocketRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.channels.Channel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class GetSockOptHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 参数顺序: opt → level → fd
        int opt = (int) stack.pop();
        int level = (int) stack.pop();
        int fd = (int) stack.pop();

        // 2. 查找 fd
        Channel ch = SocketRegistry.get(fd);
        if (ch == null) {
            throw new IllegalArgumentException("Invalid socket fd: " + fd);
        }

        // 3. 映射 option
        SocketOption<?> option = mapSockOpt(level, opt);
        if (option == null) {
            throw new UnsupportedOperationException(
                    "Unsupported socket option: level=" + level + ", opt=" + opt);
        }

        Object value;
        if (ch instanceof SocketChannel) {
            value = ((SocketChannel) ch).getOption(option);
        } else if (ch instanceof ServerSocketChannel) {
            value = ((ServerSocketChannel) ch).getOption(option);
        } else {
            throw new IllegalStateException("Unsupported channel type: " + ch.getClass());
        }

        // 4. 压回结果
        stack.push(value);
    }

    private SocketOption<?> mapSockOpt(int level, int opt) {
        // 这里需要你定义常量，比如:
        // level = SOL_SOCKET, IPPROTO_TCP ...
        // opt = SO_REUSEADDR, TCP_NODELAY ...
        // 暂时写死几个常见的
        return switch (opt) {
            case 1 -> // SO_REUSEADDR
                    StandardSocketOptions.SO_REUSEADDR;
            case 2 -> // SO_KEEPALIVE
                    StandardSocketOptions.SO_KEEPALIVE;
            case 3 -> // SO_RCVBUF
                    StandardSocketOptions.SO_RCVBUF;
            case 4 -> // SO_SNDBUF
                    StandardSocketOptions.SO_SNDBUF;
            case 5 -> // TCP_NODELAY
                    StandardSocketOptions.TCP_NODELAY;
            default -> null;
        };
    }
}
