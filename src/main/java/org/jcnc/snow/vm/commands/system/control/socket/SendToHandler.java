package org.jcnc.snow.vm.commands.system.control.socket;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.SocketRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;

public class SendToHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 参数顺序: port → addr → data → fd
        int port = (int) stack.pop();
        String addr = (String) stack.pop();
        Object dataObj = stack.pop();
        int fd = (int) stack.pop();

        // 2. 获取 DatagramChannel
        DatagramChannel channel = (DatagramChannel) SocketRegistry.get(fd);
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
            throw new IllegalArgumentException("Unsupported data type for SENDTO: " + dataObj.getClass());
        }

        // 4. 发送数据
        int sent = channel.send(ByteBuffer.wrap(bytes), new InetSocketAddress(addr, port));

        // 5. 压回返回值
        stack.push(sent);
    }
}
