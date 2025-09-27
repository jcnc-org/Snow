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
 * {@code SetSockOptHandler} 实现 SETSOCKOPT (0x140A) 系统调用，
 * 用于设置 socket 的特定选项值。
 *
 * <p><b>Stack：</b>
 * 入参 {@code (fd:int, level:int, opt:int, value:any)} →
 * 出参 {@code (rc:int)}
 * </p>
 *
 * <p><b>语义：</b>
 * 设置 fd 对应 socket 的指定选项（opt, level）为 value。
 * 支持部分标准 SocketOption，level/opt 参考 SOL_SOCKET、IPPROTO_TCP 常用值。
 * </p>
 *
 * <p><b>返回：</b>
 * 设置成功返回 {@code 0}，失败返回 {@code -1}。
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>fd 非法、通道类型错误、参数不支持或底层 I/O 错误时返回 -1</li>
 * </ul>
 * </p>
 */
public class SetSockOptHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack, LocalVariableStore locals, CallStack callStack) throws Exception {
        Object valueObj = stack.pop();
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
            Object coerced = coerceValue(option, valueObj);
            if (ch instanceof NetworkChannel nc) {
                nc.setOption((SocketOption<Object>) option, coerced);
            } else {
                stack.push(-1);
                return;
            }
            stack.push(0);
        } catch (Throwable t) {
            stack.push(-1);
        }
    }

    /**
     * level/opt 常量到 Java StandardSocketOptions 的映射。
     * 兼容 SOL_SOCKET=1, SO_REUSEADDR=2, IPPROTO_TCP=6, TCP_NODELAY=1/5。
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

    /**
     * 类型适配：不同 socket 选项需要不同类型
     */
    private Object coerceValue(SocketOption<?> option, Object raw) {
        if (option == StandardSocketOptions.SO_REUSEADDR ||
                option == StandardSocketOptions.SO_KEEPALIVE ||
                option == StandardSocketOptions.TCP_NODELAY ||
                option == StandardSocketOptions.SO_BROADCAST) {
            if (raw instanceof Boolean b) return b;
            if (raw instanceof Number n) return n.intValue() != 0;
            return Boolean.parseBoolean(String.valueOf(raw));
        }
        if (option == StandardSocketOptions.SO_RCVBUF ||
                option == StandardSocketOptions.SO_SNDBUF) {
            if (raw instanceof Number n) return n.intValue();
            return Integer.parseInt(String.valueOf(raw));
        }
        return raw;
    }
}
