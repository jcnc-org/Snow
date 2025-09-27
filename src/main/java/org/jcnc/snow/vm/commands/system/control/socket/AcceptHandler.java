package org.jcnc.snow.vm.commands.system.control.socket;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.SocketRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * {@code AcceptHandler} 实现 ACCEPT (0x1403) 系统调用，
 * 用于接受一个传入的 socket 连接。
 *
 * <p><b>Stack</b>：入参 {@code (fd:int)} → 出参 {@code (cfd:int, addr:String, port:int)}</p>
 *
 * <p><b>语义</b>：阻塞等待并接受传入连接，返回新连接 fd、对端地址和端口。</p>
 *
 * <p><b>返回</b>：
 * <ul>
 *   <li>cfd：新连接的 socket fd</li>
 *   <li>addr：对端 IP 地址（String）</li>
 *   <li>port：对端端口号（int）</li>
 * </ul>
 * </p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>fd 无效时抛出 {@link IllegalArgumentException}</li>
 *   <li>accept 失败或返回 null 时抛出 {@link RuntimeException}</li>
 * </ul>
 * </p>
 */
public class AcceptHandler implements SyscallHandler {

    /**
     * 处理 ACCEPT 调用。
     *
     * @param stack     操作数栈，提供 server fd，并返回新连接 fd、地址、端口
     * @param locals    局部变量存储器（未使用）
     * @param callStack 调用栈（未使用）
     * @throws Exception fd 无效或 accept 失败时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 取入参：监听 socket 的 fd
        Object fdObj = stack.pop();
        if (!(fdObj instanceof Number)) {
            throw new IllegalArgumentException("ACCEPT: fd must be an int, got: " + fdObj);
        }
        int fd = ((Number) fdObj).intValue();

        // 2. 取到 ServerSocketChannel
        ServerSocketChannel server = (ServerSocketChannel) SocketRegistry.get(fd);
        if (server == null) {
            throw new IllegalArgumentException("ACCEPT: invalid listen fd: " + fd);
        }

        // 3. 阻塞接受一个连接（SocketHandler中已将通道设为blocking=true）
        SocketChannel channel = server.accept();
        if (channel == null) {
            // 理论上阻塞模式不会为 null；若实现改为非阻塞，这里可返回 -1 或抛异常
            stack.push(-1);
            return;
        }
        channel.configureBlocking(true);

        // 4. 注册到 SocketRegistry，获得新的 cfd
        int cfd = SocketRegistry.register(channel);

        // 5. 返回 cfd
        stack.push(cfd);
    }
}
