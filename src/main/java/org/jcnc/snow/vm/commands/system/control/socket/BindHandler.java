package org.jcnc.snow.vm.commands.system.control.socket;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.SocketRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;

public class BindHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 从操作数栈取出参数 (注意顺序：先 pop port，再 pop addr，再 pop fd)
        int port = (int) stack.pop();
        String addr = (String) stack.pop();
        int fd = (int) stack.pop();

        // 2. 获取 ServerSocketChannel
        ServerSocketChannel server = (ServerSocketChannel) SocketRegistry.get(fd);
        if (server == null) {
            throw new IllegalArgumentException("Invalid socket fd: " + fd);
        }

        // 3. 绑定地址和端口
        server.bind(new InetSocketAddress(addr, port));

        // 4. 返回 0
        stack.push(0);
    }
}
