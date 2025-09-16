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
 * {@code RecvFromHandler} 实现 RECVFROM (0x1409) 系统调用，
 * 用于从 UDP socket 接收数据包及对端地址。
 *
 * <p><b>Stack</b>：入参 {@code (fd:int, n:int)} → 出参 {@code (data:byte[], addr:String, port:int)}</p>
 *
 * <p><b>语义</b>：接收至多 n 字节的数据，并返回数据内容、对端 IP 和端口。</p>
 *
 * <p><b>返回</b>：
 * <ul>
 *   <li>data：收到的数据（byte[]）</li>
 *   <li>addr：对端 IP 地址（String）</li>
 *   <li>port：对端端口号（int）</li>
 * </ul>
 * </p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>fd 无效时抛出 {@link IllegalArgumentException}</li>
 *   <li>接收失败时抛出 {@link java.io.IOException}</li>
 * </ul>
 * </p>
 */
public class RecvFromHandler implements SyscallHandler {

    /**
     * 处理 RECVFROM 调用。
     *
     * @param stack     操作数栈，依次提供 fd、n
     * @param locals    局部变量存储器（未使用）
     * @param callStack 调用栈（未使用）
     * @throws Exception fd 无效或接收失败时抛出
     */
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

        // 5. 压回返回值: data, addr, port
        stack.push(data);
        stack.push(addr);
        stack.push(port);
    }
}
