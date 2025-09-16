package org.jcnc.snow.vm.commands.system.control.socket;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.SocketRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class ConnectHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 从操作数栈取参数 (顺序: port → addr → fd)
        int port = (int) stack.pop();
        String addr = (String) stack.pop();
        int fd = (int) stack.pop();

        // 2. 获取 SocketChannel
        SocketChannel client = (SocketChannel) SocketRegistry.get(fd);
        if (client == null) {
            throw new IllegalArgumentException("Invalid socket fd: " + fd);
        }

        // 3. 执行连接
        client.connect(new InetSocketAddress(addr, port));

        // 4. 成功后压回 0
        stack.push(0);
    }
}
