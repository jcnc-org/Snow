package org.jcnc.snow.vm.commands.system.control.socket;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.SocketRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class RecvHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 参数顺序: n → fd
        int n = (int) stack.pop();
        int fd = (int) stack.pop();

        // 2. 获取 SocketChannel
        SocketChannel channel = (SocketChannel) SocketRegistry.get(fd);
        if (channel == null) {
            throw new IllegalArgumentException("Invalid socket fd: " + fd);
        }

        // 3. 接收数据
        ByteBuffer buffer = ByteBuffer.allocate(n);
        int read = channel.read(buffer);

        byte[] data;
        if (read == -1) {
            // 对端关闭，返回空字节数组
            data = new byte[0];
        } else {
            buffer.flip();
            data = new byte[buffer.remaining()];
            buffer.get(data);
        }

        // 4. 压回结果
        stack.push(data);
    }
}
