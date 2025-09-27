package org.jcnc.snow.vm.commands.system.control.socket;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.SocketRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.channels.Channel;
import java.nio.channels.NetworkChannel;

/**
 * {@code GetSockOptHandler} 实现 GETSOCKOPT (0x140B) 系统调用，
 * 用于查询 socket 的选项值（如 SO_REUSEADDR、SO_RCVBUF 等）。
 *
 * <p><b>Stack：</b>
 * 入参 {@code (fd:int, level:int, opt:int)} →
 * 出参 {@code (value:any)}
 * </p>
 *
 * <p><b>语义：</b>
 * 获取指定 fd 对应的 socket 选项（level、opt），支持部分标准选项。
 * 支持 SOL_SOCKET/ IPPROTO_TCP 的常见选项映射到 Java {@link StandardSocketOptions}。
 * </p>
 *
 * <p><b>返回：</b>
 * 成功时返回选项值（类型视选项不同而异）。
 * 失败时返回 -1。
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>fd 非法、通道类型错误、参数不支持或底层 I/O 错误时返回 -1</li>
 * </ul>
 * </p>
 */
public class GetSockOptHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack, LocalVariableStore locals, CallStack callStack) throws Exception {
        Object optObj = stack.pop();
        Object levelObj = stack.pop();
        Object fdObj = stack.pop();

        int fd = ((Number) fdObj).intValue();
        int level = ((Number) levelObj).intValue();
        int opt = ((Number) optObj).intValue();

        Channel ch = SocketRegistry.get(fd);
        if (ch == null) {
            stack.push(-1);
            return;
        }

        SocketOption<?> option = mapSockOpt(level, opt);
        if (option == null) {
            stack.push(-1);
            return;
        }

        try {
            if (ch instanceof NetworkChannel nc) {
                Object val = nc.getOption((SocketOption<Object>) option);
                stack.push(val);
            } else {
                stack.push(-1);
            }
        } catch (Throwable t) {
            stack.push(-1);
        }
    }

    /**
     * C/POSIX socket 选项常量到 Java StandardSocketOptions 的映射。
     */
    private SocketOption<?> mapSockOpt(int level, int opt) {
        if (level == 1) { // SOL_SOCKET
            return switch (opt) {
                case 2 -> StandardSocketOptions.SO_REUSEADDR;
                case 3 -> StandardSocketOptions.SO_BROADCAST;
                case 4 -> StandardSocketOptions.SO_RCVBUF;
                case 5 -> StandardSocketOptions.SO_SNDBUF;
                default -> null;
            };
        }
        if (level == 6) { // IPPROTO_TCP
            return switch (opt) {
                case 1, 5 -> StandardSocketOptions.TCP_NODELAY;
                default -> null;
            };
        }
        return null;
    }
}
