package org.jcnc.snow.vm.commands.system.control.socket;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.SocketRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class RecvFromHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 参数顺序: n → fd
        int n = (int) stack.pop();
        int fd = (int) stack.pop();

        // 2. 获取 DatagramChannel
        DatagramChannel channel = (DatagramChannel) SocketRegistry.get(fd);
        if (channel == null) {
            throw new IllegalArgumentException("Invalid socket fd: " + fd);
        }

        // 3. 接收数据
        ByteBuffer buffer = ByteBuffer.allocate(n);
        SocketAddress remote = channel.receive(buffer);

        // 4. 提取数据
        buffer.flip();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);

        String addr = "";
        int port = 0;
        if (remote instanceof InetSocketAddress) {
            InetSocketAddress inet = (InetSocketAddress) remote;
            addr = inet.getAddress().getHostAddress();
            port = inet.getPort();
        }

        // 5. 压回返回值: data, addr, port
        stack.push(data);
        stack.push(addr);
        stack.push(port);
    }
}
