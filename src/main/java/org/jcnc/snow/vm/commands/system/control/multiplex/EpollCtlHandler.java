package org.jcnc.snow.vm.commands.system.control.multiplex;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.EpollRegistry;
import org.jcnc.snow.vm.io.EpollInstance;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code EpollCtlHandler} 实现 EPOLL_CTL (0x1302) 系统调用，
 * 用于管理 epoll 实例中的监控项。
 *
 * <p><b>Stack</b>：入参 {@code (epfd:int, op:int, fd:int, events:int)} → 出参 {@code (rc:int)}</p>
 *
 * <p><b>语义</b>：</p>
 * <ul>
 *   <li>op=1 (ADD)：将 fd 添加到 epoll，关注指定事件</li>
 *   <li>op=2 (MOD)：修改已存在 fd 的事件掩码</li>
 *   <li>op=3 (DEL)：移除 fd 的监控</li>
 * </ul>
 *
 * <p><b>返回</b>：成功返回 {@code 0}。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>epfd 无效 → {@link IllegalArgumentException}</li>
 *   <li>op 非法 → {@link IllegalArgumentException}</li>
 *   <li>其它底层错误 → {@link Exception}</li>
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

        // 参数出栈顺序：events, fd, op, epfd
        int events = (int) stack.pop();
        int fd     = (int) stack.pop();
        int op     = (int) stack.pop();
        int epfd   = (int) stack.pop();

        EpollInstance instance = EpollRegistry.get(epfd);

        switch (op) {
            case EPOLL_CTL_ADD -> instance.addOrUpdate(fd, events);
            case EPOLL_CTL_MOD -> {
                if (!instance.getWatchMap().containsKey(fd)) {
                    throw new IllegalArgumentException("EPOLL_CTL_MOD: fd not registered " + fd);
                }
                instance.addOrUpdate(fd, events);
            }
            case EPOLL_CTL_DEL -> instance.remove(fd);
            default -> throw new IllegalArgumentException("Invalid epoll_ctl op: " + op);
        }

        // 成功返回 0
        stack.push(0);
    }
}
