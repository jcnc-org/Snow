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

/**
 * {@code SendToHandler} 实现 SENDTO (0x140C) 系统调用，
 * 用于通过 UDP socket 发送数据到指定目标地址和端口。
 *
 * <p><b>Stack</b>：入参 {@code (fd:int, data:byte[]/String, addr:String, port:int)} → 出参 {@code (sent:int)}</p>
 *
 * <p><b>语义</b>：向指定 IP/端口发送数据。</p>
 *
 * <p><b>返回</b>：实际发送的字节数（int）。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>fd 无效时抛出 {@link IllegalArgumentException}</li>
 *   <li>数据类型不支持时抛出 {@link IllegalArgumentException}</li>
 *   <li>发送失败时抛出 {@link java.io.IOException}</li>
 * </ul>
 * </p>
 */
public class SendToHandler implements SyscallHandler {

    /**
     * 处理 SENDTO 调用。
     *
     * @param stack     操作数栈，依次提供 fd、data、addr、port
     * @param locals    局部变量存储器（未使用）
     * @param callStack 调用栈（未使用）
     * @throws Exception fd 无效、数据类型不支持或发送失败时抛出
     */
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
