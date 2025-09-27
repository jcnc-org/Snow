package org.jcnc.snow.vm.commands.system.control.socket;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.SocketRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * {@code ShutdownHandler} 实现 SHUTDOWN (0x1409) 系统调用，
 * 用于关闭 TCP/UDP socket 的输入、输出或双向流。
 *
 * <p><b>Stack：</b>
 * 入参 {@code (fd:int, how:int)} →
 * 出参 {@code (rc:int)}
 * </p>
 *
 * <p><b>语义：</b>
 * 根据 {@code how} 参数，关闭 fd 指定 socket 的输入流、输出流或全部（0=IN, 1=OUT, 2=BOTH）。
 * 支持 TCP/UDP/Server socket。
 * </p>
 *
 * <p><b>返回：</b>
 * 成功返回 {@code 0}，失败返回 {@code -1}。
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>fd 非法、通道类型不支持、参数 how 非法或关闭失败时，返回 -1</li>
 * </ul>
 * </p>
 */
public class ShutdownHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack, LocalVariableStore locals, CallStack callStack) throws Exception {
        Object howObj = stack.pop();
        Object fdObj = stack.pop();

        int fd = ((Number) fdObj).intValue();
        int how = ((Number) howObj).intValue();

        Channel ch = SocketRegistry.get(fd);
        if (ch == null) {
            stack.push(-1);
            return;
        }

        try {
            switch (ch) {
                case SocketChannel sc -> {
                    switch (how) {
                        case 0 -> sc.socket().shutdownInput();
                        case 1 -> sc.socket().shutdownOutput();
                        case 2 -> {
                            sc.socket().shutdownInput();
                            sc.socket().shutdownOutput();
                        }
                        default -> throw new IllegalArgumentException("Invalid how value: " + how);
                    }
                }
                case DatagramChannel dc -> dc.close(); // UDP: 视为关闭
                case ServerSocketChannel ssc -> ssc.close();
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
