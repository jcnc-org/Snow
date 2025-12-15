package org.jcnc.snow.vm.commands.system.control.socket;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.SocketRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;
import org.jcnc.snow.vm.runtime.SnowArrayObject;
import org.jcnc.snow.vm.runtime.SnowBytesObject;
import org.jcnc.snow.vm.runtime.SnowRuntime;
import org.jcnc.snow.vm.runtime.SnowStringObject;
import org.jcnc.snow.vm.value.ByteValue;
import org.jcnc.snow.vm.value.IntValue;
import org.jcnc.snow.vm.value.LongValue;
import org.jcnc.snow.vm.value.RefValue;
import org.jcnc.snow.vm.value.ShortValue;
import org.jcnc.snow.vm.value.Value;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.List;

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

        Value nV = stack.popValue();
        Value fdV = stack.popValue();
        int n = asInt(nV, "RECVFROM: n");
        int fd = asInt(fdV, "RECVFROM: fd");

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

        int dataId = SnowRuntime.get().heap().alloc(new SnowBytesObject(data));
        int addrId = SnowRuntime.get().heap().alloc(new SnowStringObject(addr));
        int tupId = SnowRuntime.get().heap().alloc(new SnowArrayObject(List.of(
                new RefValue(dataId),
                new RefValue(addrId),
                new IntValue(port)
        )));
        stack.pushValue(new RefValue(tupId));
    }

    private static int asInt(Value v, String what) {
        return switch (v) {
            case IntValue(int i) -> i;
            case ShortValue(short s) -> s;
            case ByteValue(byte b) -> b;
            case LongValue(long l) -> (int) l;
            default -> throw new IllegalArgumentException(what + " must be int");
        };
    }
}