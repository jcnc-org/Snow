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

public class SetSockOptHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 参数顺序: value → opt → level → fd
        Object value = stack.pop();
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

        // 4. 设置选项
        if (ch instanceof SocketChannel) {
            ((SocketChannel) ch).setOption((SocketOption<Object>) option, value);
        } else if (ch instanceof ServerSocketChannel) {
            ((ServerSocketChannel) ch).setOption((SocketOption<Object>) option, value);
        } else {
            throw new IllegalStateException("Unsupported channel type: " + ch.getClass());
        }

        // 5. 返回 0
        stack.push(0);
    }

    private SocketOption<?> mapSockOpt(int level, int opt) {
        // 这里需要你在 VM 常量表里定义 opt 值，例如：
        // 1 = SO_REUSEADDR, 2 = SO_KEEPALIVE, 3 = SO_RCVBUF, 4 = SO_SNDBUF, 5 = TCP_NODELAY
        switch (opt) {
            case 1: // SO_REUSEADDR
                return StandardSocketOptions.SO_REUSEADDR;
            case 2: // SO_KEEPALIVE
                return StandardSocketOptions.SO_KEEPALIVE;
            case 3: // SO_RCVBUF
                return StandardSocketOptions.SO_RCVBUF;
            case 4: // SO_SNDBUF
                return StandardSocketOptions.SO_SNDBUF;
            case 5: // TCP_NODELAY
                return StandardSocketOptions.TCP_NODELAY;
            case 6: // SO_BROADCAST (for UDP)
                return StandardSocketOptions.SO_BROADCAST;
            default:
                return null;
        }
    }
}
