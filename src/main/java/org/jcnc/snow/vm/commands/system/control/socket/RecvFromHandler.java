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

/**
 * {@code RecvFromHandler} 实现 RECVFROM (0x1408) 系统调用，
 * 用于从 UDP socket 接收数据包及对端地址信息。
 *
 * <p><b>Stack：</b>
 * 入参 {@code (fd:int, n:int)} →
 * 出参 {@code (tuple:any[])}
 * </p>
 *
 * <p><b>语义：</b>
 * 从 fd 指定的 UDP socket 接收最多 n 字节数据，返回一个三元组数组：
 * <ul>
 *   <li>[0] = data: byte[]</li>
 *   <li>[1] = addr: String</li>
 *   <li>[2] = port: int</li>
 * </ul>
 * </p>
 *
 * <p><b>返回：</b>
 * 返回 {data, addr, port} 数组。若接收失败，data 为空，addr/port 为空或0。
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>fd 非法、非 DatagramChannel 或接收异常时抛出 {@link IllegalArgumentException} 或 I/O 异常。</li>
 * </ul>
 * </p>
 */
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
        if (remote instanceof InetSocketAddress inet) {
            addr = inet.getAddress().getHostAddress();
            port = inet.getPort();
        }

        // 5. 返回一个数组: {data, addr, port}
        Object[] result = new Object[]{data, addr, port};
        stack.push(result);
    }
}
