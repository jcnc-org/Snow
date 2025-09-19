package org.jcnc.snow.vm.commands.system.control.socket;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.SocketRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.net.SocketOption;
import java.net.StandardSocketOptions;
import java.nio.channels.Channel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * {@code SetSockOptHandler} 实现 SETSOCKOPT (0x140D) 系统调用，
 * 用于设置 socket 的特定选项值。
 *
 * <p><b>Stack</b>：入参 {@code (fd:int, level:int, opt:int, value:any)} → 出参 {@code (0:int)}</p>
 *
 * <p><b>语义</b>：为 fd 指定的 socket 设置选项（opt，level）为指定 value。</p>
 *
 * <p><b>返回</b>：始终返回 0。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>fd 无效时抛出 {@link IllegalArgumentException}</li>
 *   <li>socket 选项不支持时抛出 {@link UnsupportedOperationException}</li>
 *   <li>channel 类型不支持时抛出 {@link IllegalStateException}</li>
 * </ul>
 * </p>
 */
public class SetSockOptHandler implements SyscallHandler {

    /**
     * 处理 SETSOCKOPT 调用。
     *
     * @param stack     操作数栈，依次提供 fd、level、opt、value
     * @param locals    局部变量存储器（未使用）
     * @param callStack 调用栈（未使用）
     * @throws Exception fd 无效、选项不支持或类型错误时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 参数顺序: value → opt → level → fd
        Object value = stack.pop();
        int opt = (int) stack.pop();
        int level = (int) stack.pop();
        int fd = (int) stack.pop();

        // 2. 查找 fd
        Channel ch = SocketRegistry.get(fd);
        if (ch == null) {
            throw new IllegalArgumentException("Invalid socket fd: " + fd);
        }

        // 3. 映射 option
        SocketOption<?> option = mapSockOpt(level, opt);
        if (option == null) {
            throw new UnsupportedOperationException(
                    "Unsupported socket option: level=" + level + ", opt=" + opt);
        }

        // 4. 设置选项
        if (ch instanceof SocketChannel) {
            ((SocketChannel) ch).setOption((SocketOption<Object>) option, value);
        } else if (ch instanceof ServerSocketChannel) {
            ((ServerSocketChannel) ch).setOption((SocketOption<Object>) option, value);
        } else {
            throw new IllegalStateException("Unsupported channel type: " + ch.getClass());
        }

        // 5. 返回 0
        stack.push(0);
    }

    /**
     * 映射用户自定义的 opt/level 到 Java SocketOption
     */
    private SocketOption<?> mapSockOpt(int level, int opt) {
        // 这里只是举例，建议你将 level/opt 常量统一在 VM 协议常量表定义
        return switch (opt) {
            case 1 -> StandardSocketOptions.SO_REUSEADDR;
            case 2 -> StandardSocketOptions.SO_KEEPALIVE;
            case 3 -> StandardSocketOptions.SO_RCVBUF;
            case 4 -> StandardSocketOptions.SO_SNDBUF;
            case 5 -> StandardSocketOptions.TCP_NODELAY;
            case 6 -> StandardSocketOptions.SO_BROADCAST; // for UDP
            default -> null;
        };
    }
}
