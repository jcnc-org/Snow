package org.jcnc.snow.vm.commands.system.control.socket;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.SocketRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

/**
 * {@code SendHandler} 实现 SEND系统调用，
 * 用于向 TCP socket 发送数据。
 *
 * <p><b>Stack</b>：入参 {@code (fd:int, data:byte[]/String)} → 出参 {@code (written:int)}</p>
 *
 * <p><b>语义</b>：向 fd 指定的 SocketChannel 发送字节数据或字符串数据。</p>
 *
 * <p><b>返回</b>：实际写入的字节数（int）。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>fd 无效时抛出 {@link IllegalArgumentException}</li>
 *   <li>数据类型不支持时抛出 {@link IllegalArgumentException}</li>
 *   <li>写入失败时抛出 {@link java.io.IOException}</li>
 * </ul>
 * </p>
 */
public class SendHandler implements SyscallHandler {

    /**
     * 处理 SEND 调用。
     *
     * @param stack     操作数栈，依次提供 fd、data
     * @param locals    局部变量存储器（未使用）
     * @param callStack 调用栈（未使用）
     * @throws Exception fd 无效、数据类型不支持或写入失败时抛出
     */
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
