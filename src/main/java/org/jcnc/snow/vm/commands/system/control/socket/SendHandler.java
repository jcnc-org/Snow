package org.jcnc.snow.vm.commands.system.control.socket;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.SocketRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class SendHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 参数顺序: data → fd
        Object dataObj = stack.pop();
        int fd = (int) stack.pop();

        // 2. 获取 SocketChannel
        SocketChannel channel = (SocketChannel) SocketRegistry.get(fd);
        if (channel == null) {
            throw new IllegalArgumentException("Invalid socket fd: " + fd);
        }

        // 3. 转换数据
        byte[] bytes;
        if (dataObj instanceof byte[]) {
            bytes = (byte[]) dataObj;
        } else if (dataObj instanceof String) {
            bytes = ((String) dataObj).getBytes(StandardCharsets.UTF_8);
        } else {
            throw new IllegalArgumentException("Unsupported data type for SEND: " + dataObj.getClass());
        }

        // 4. 写入数据
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int written = channel.write(buffer);

        // 5. 压回写入的字节数
        stack.push(written);
    }
}
