package org.jcnc.snow.vm.commands.system.control.socket;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.SocketRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * {@code RecvHandler} 实现 RECV (0x140A) 系统调用，
 * 用于从 TCP socket 读取数据。
 *
 * <p><b>Stack</b>：入参 {@code (fd:int, n:int)} → 出参 {@code (data:byte[])}</p>
 *
 * <p><b>语义</b>：从 fd 指定的 SocketChannel 接收至多 n 字节的数据。</p>
 *
 * <p><b>返回</b>：收到的数据（byte[]）。如果对端已关闭，则返回空字节数组。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>fd 无效时抛出 {@link IllegalArgumentException}</li>
 *   <li>接收失败时抛出 {@link java.io.IOException}</li>
 * </ul>
 * </p>
 */
public class RecvHandler implements SyscallHandler {

    /**
     * 处理 RECV 调用。
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
