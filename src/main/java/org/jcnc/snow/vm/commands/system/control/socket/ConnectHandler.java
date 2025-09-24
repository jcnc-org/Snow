package org.jcnc.snow.vm.commands.system.control.socket;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.SocketRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * {@code ConnectHandler} 实现 CONNECT (0x1404) 系统调用，
 * 用于将 socket fd 连接到指定远程地址与端口。
 *
 * <p><b>Stack：</b>
 * 入参 {@code (fd:int, addr:String, port:int)} →
 * 出参 {@code (rc:int=0)}
 * </p>
 *
 * <p><b>语义：</b>
 * 连接 fd 指定的 socket 到远程 {@code addr:port}。
 * 若 fd 尚未绑定 {@link SocketChannel}，会自动创建新通道（或替换掉原有 ServerSocketChannel）。
 * </p>
 *
 * <p><b>返回：</b>
 * 连接成功返回 {@code 0}。
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>fd 非法、channel 类型不支持或参数类型错误时抛出 {@link IllegalArgumentException}</li>
 *   <li>底层 I/O 错误可能抛出 {@link java.io.IOException}</li>
 * </ul>
 * </p>
 */
public class ConnectHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 按栈顺序，最后入栈的参数先弹出
        Object portObj = stack.pop();   // port
        Object addrObj = stack.pop();   // addr
        Object fdObj = stack.pop();   // fd

        if (!(fdObj instanceof Number)) {
            throw new IllegalArgumentException("CONNECT: fd must be an int, got: " + fdObj);
        }
        int fd = ((Number) fdObj).intValue();

        String addr = String.valueOf(addrObj);
        int port;
        if (portObj instanceof Number) {
            port = ((Number) portObj).intValue();
        } else {
            try {
                port = Integer.parseInt(String.valueOf(portObj));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("CONNECT: port must be int, got: " + portObj);
            }
        }

        // 取出当前注册在该 fd 下的通道
        Channel ch = SocketRegistry.get(fd);

        SocketChannel sc;
        switch (ch) {
            case null -> {
                // 若 fd 尚未关联通道，则新建 SocketChannel 并注册
                sc = SocketChannel.open();
                sc.configureBlocking(true);
                SocketRegistry.replace(fd, sc);
            }
            case SocketChannel socketChannel -> sc = socketChannel;
            case ServerSocketChannel serverSocketChannel -> {
                // 如果是服务端 channel，先关闭并替换为 SocketChannel
                try {
                    ch.close();
                } catch (Exception ignore) {
                }
                sc = SocketChannel.open();
                sc.configureBlocking(true);
                SocketRegistry.replace(fd, sc);
            }
            default -> throw new IllegalArgumentException("CONNECT: unsupported channel type for fd "
                    + fd + ": " + ch.getClass());
        }

        // 执行连接
        sc.connect(new InetSocketAddress(addr, port));

        // 成功返回 0
        stack.push(0);
    }
}
