package org.jcnc.snow.vm.commands.system.control.socket;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.SocketRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.channels.SocketChannel;

public class ShutdownHandler implements SyscallHandler {
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
