package org.jcnc.snow.vm.commands.system.control.socket;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.SocketRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * {@code BindHandler} 实现 BIND (0x1401) 系统调用，
 * 负责将 socket fd 绑定到本地地址（支持 TCP/UDP/Server）。
 *
 * <p><b>Stack：</b>
 * 入参 {@code (fd:int, addr:String, port:int)} →
 * 出参 {@code (rc:int)}
 * </p>
 *
 * <p><b>语义：</b>
 * 绑定指定 fd 对应的 socket 到本地地址 {@code addr:port}。
 * 支持 TCP 客户端/服务端与 UDP socket。
 * </p>
 *
 * <p><b>返回：</b>
 * 成功返回 {@code 0}，失败返回 {@code -1}。
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>fd 非法或未注册时，返回 -1</li>
 *   <li>底层 bind 失败或参数错误时，返回 -1</li>
 * </ul>
 * </p>
 */
public class BindHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack, LocalVariableStore locals, CallStack callStack) throws Exception {
        Object portObj = stack.pop();
        Object addrObj = stack.pop();
        Object fdObj = stack.pop();

        int fd = ((Number) fdObj).intValue();
        String addr = String.valueOf(addrObj);
        int port = ((Number) portObj).intValue();

        Channel ch = SocketRegistry.get(fd);
        if (ch == null) {
            stack.push(-1);
            return;
        }

        try {
            InetSocketAddress isa = new InetSocketAddress(addr, port);
            switch (ch) {
                case ServerSocketChannel ssc -> ssc.bind(isa);
                case DatagramChannel dgc -> dgc.bind(isa);
                case SocketChannel sc -> sc.bind(isa); // 客户端 socket 允许绑定本地端口
                default -> {
                    stack.push(-1);
                    return;
                }
            }
            stack.push(0);
        } catch (Throwable t) {
            stack.push(-1);
        }
    }
}
