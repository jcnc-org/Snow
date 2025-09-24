package org.jcnc.snow.vm.commands.system.control.socket;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.SocketRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

/**
 * {@code ConnectHandler} 实现 CONNECT (0x1404) 系统调用，
 * 用于将 socket 连接到指定的远程地址和端口。
 *
 * <p><b>Stack</b>：入参 {@code (fd:int, addr:String, port:int)} → 出参 {@code (0:int)}</p>
 *
 * <p><b>语义</b>：将 fd 指定的 SocketChannel 连接到 addr:port。</p>
 *
 * <p><b>返回</b>：始终返回 0。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>fd 无效时抛出 {@link IllegalArgumentException}</li>
 *   <li>连接失败时抛出 {@link java.io.IOException}</li>
 * </ul>
 * </p>
 */
public class ConnectHandler implements SyscallHandler {

    /**
     * 处理 CONNECT 调用。
     *
     * @param stack     操作数栈，依次提供 fd、addr、port
     * @param locals    局部变量存储器（未使用）
     * @param callStack 调用栈（未使用）
     * @throws Exception fd 无效或连接失败时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 从操作数栈取参数 (顺序: port → addr → fd)
        int port = (int) stack.pop();
        String addr = (String) stack.pop();
        int fd = (int) stack.pop();

        // 2. 获取 SocketChannel
        SocketChannel client = (SocketChannel) SocketRegistry.get(fd);
        if (client == null) {
            throw new IllegalArgumentException("Invalid socket fd: " + fd);
        }

        // 3. 执行连接
        client.connect(new InetSocketAddress(addr, port));

        // 4. 成功后压回 0
        stack.push(0);
    }
}
