package org.jcnc.snow.vm.commands.system.control.socket;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.SocketRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;

import static org.jcnc.snow.vm.io.SocketConstants.*;

/**
 * {@code SocketHandler} 实现 SOCKET (0x1400) 系统调用，
 * 用于创建 TCP/UDP socket，并返回 socket 文件描述符。
 *
 * <p><b>Stack</b>：入参 {@code (family:int, type:int, proto:int)} → 出参 {@code (fd:int)}</p>
 *
 * <p><b>语义</b>：根据协议族 family、类型 type 创建新的 socket，并返回 fd。</p>
 *
 * <p><b>返回</b>：新分配的 socket fd（int）。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>family 或 type 不支持时抛出 {@link UnsupportedOperationException}</li>
 *   <li>创建 socket 失败时抛出 {@link java.io.IOException}</li>
 * </ul>
 * </p>
 */
public class SocketHandler implements SyscallHandler {
    /**
     * 处理 SOCKET 调用。
     *
     * @param stack     操作数栈，依次提供 family、type、proto
     * @param locals    局部变量存储器（未使用）
     * @param callStack 调用栈（未使用）
     * @throws Exception 参数非法或创建失败时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 参数顺序: proto → type → family
        int proto = (int) stack.pop();
        int type = (int) stack.pop();
        int family = (int) stack.pop();

        // 2. 检查 family
        if (family != AF_INET && family != AF_INET6) {
            throw new UnsupportedOperationException("Unsupported family: " + family);
        }

        int fd;

        // 3. 根据 type 创建不同的 Channel
        if (type == SOCK_STREAM) {
            // TCP（默认创建 ServerSocketChannel，供 bind/listen/accept 使用）
            ServerSocketChannel server = ServerSocketChannel.open();
            server.configureBlocking(true);
            fd = SocketRegistry.register(server);
        } else if (type == SOCK_DGRAM) {
            // UDP
            DatagramChannel udp = DatagramChannel.open();
            udp.configureBlocking(true);
            fd = SocketRegistry.register(udp);
        } else {
            throw new UnsupportedOperationException("Unsupported socket type: " + type);
        }

        // 4. 压回 fd
        stack.push(fd);
    }
}
