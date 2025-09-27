package org.jcnc.snow.vm.commands.system.control.multiplex;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.EpollInstance;
import org.jcnc.snow.vm.io.EpollRegistry;
import org.jcnc.snow.vm.io.FDTable;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.channels.Channel;
import java.nio.channels.SelectableChannel;

/**
 * {@code EpollCtlHandler} 实现 EPOLL_CTL (0x1302) 系统调用，
 * 支持 epoll 实例内的 fd 监控项增删改操作，并兼容标准流 fd=0/1/2。
 *
 * <p><b>Stack：</b>
 * 入参 {@code (epfd:int, op:int, fd:int, events:int)} →
 * 出参 {@code (rc:int)}
 * </p>
 *
 * <p><b>语义：</b>
 * 管理 epoll 实例中监控的文件描述符（fd），可进行如下操作：
 * <ul>
 *   <li>{@code op=1 (ADD)} — 添加 fd，关注指定事件</li>
 *   <li>{@code op=2 (MOD)} — 修改已注册 fd 的事件掩码</li>
 *   <li>{@code op=3 (DEL)} — 移除 fd 的监控</li>
 * </ul>
 * 支持标准流 fd=0/1/2 伪通道（stdin/stdout/stderr）。
 * 其它 fd 要求为 {@link SelectableChannel}。
 * 事件掩码：1=READ，2=WRITE，4=CONNECT。
 * </p>
 *
 * <p><b>返回：</b>
 * 成功返回 {@code 0}。
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>epfd 无效时抛出 {@link IllegalArgumentException}</li>
 *   <li>op 非法、fd 未注册 (MOD/DEL)、fd 不支持时抛出 {@link IllegalArgumentException}</li>
 *   <li>其它底层错误也可能抛出异常</li>
 * </ul>
 * </p>
 */
public class EpollCtlHandler implements SyscallHandler {

    public static final int EPOLL_CTL_ADD = 1;
    public static final int EPOLL_CTL_MOD = 2;
    public static final int EPOLL_CTL_DEL = 3;

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 出栈顺序：events, fd, op, epfd（逆序参数）
        int events = (int) stack.pop();
        int fd = (int) stack.pop();
        int op = (int) stack.pop();
        int epfd = (int) stack.pop();

        // 获取 epoll 实例
        EpollInstance instance = EpollRegistry.get(epfd);

        // 兼容标准流 fd 0/1/2
        if (fd == 0 || fd == 1 || fd == 2) {
            switch (op) {
                case EPOLL_CTL_ADD, EPOLL_CTL_MOD -> {
                    instance.addOrUpdatePseudo(fd, events);
                }
                case EPOLL_CTL_DEL -> {
                    if (!instance.containsPseudoFd(fd)) {
                        throw new IllegalArgumentException("EPOLL_CTL_DEL: pseudo fd not registered " + fd);
                    }
                    instance.removePseudo(fd);
                }
                default -> throw new IllegalArgumentException("Invalid epoll_ctl op: " + op);
            }
            stack.push(0);
            return;
        }

        // 其它 fd 还是用 SelectableChannel
        Channel ch = FDTable.get(fd);
        if (!(ch instanceof SelectableChannel)) {
            throw new IllegalArgumentException("epoll_ctl: fd " + fd + " is not a selectable channel and not a pseudo-fd");
        }

        switch (op) {
            case EPOLL_CTL_ADD -> instance.addOrUpdate(fd, events); // 新增监控
            case EPOLL_CTL_MOD -> {
                if (!instance.containsFd(fd)) {
                    throw new IllegalArgumentException("EPOLL_CTL_MOD: fd not registered " + fd);
                }
                instance.addOrUpdate(fd, events);
            }
            case EPOLL_CTL_DEL -> {
                if (!instance.containsFd(fd)) {
                    throw new IllegalArgumentException("EPOLL_CTL_DEL: fd not registered " + fd);
                }
                instance.remove(fd);
            }
            default -> throw new IllegalArgumentException("Invalid epoll_ctl op: " + op);
        }

        // 成功返回 0
        stack.push(0);
    }
}
