package org.jcnc.snow.vm.commands.system.control.multiplex;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.EpollRegistry;
import org.jcnc.snow.vm.io.EpollInstance;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code EpollCtlHandler} 实现 EPOLL_CTL (0x1302) 系统调用，
 * 支持 epoll 实例内的 fd 监控项增删改操作。
 *
 * <p>
 * <b>参数栈：</b> 入参 (epfd:int, op:int, fd:int, events:int)，出参 (rc:int)
 * </p>
 *
 * <p>
 * <b>op：</b>
 * <ul>
 *   <li>(ADD)：添加 fd 到 epoll，关注指定事件</li>
 *   <li>(MOD)：修改已存在 fd 的事件掩码</li>
 *   <li>(DEL)：移除 fd 的监控</li>
 * </ul>
 * </p>
 *
 * <p>成功返回 0，失败抛出 IllegalArgumentException。</p>
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
        int fd     = (int) stack.pop();
        int op     = (int) stack.pop();
        int epfd   = (int) stack.pop();

        // 获取 epoll 实例
        EpollInstance instance = EpollRegistry.get(epfd);

        // 根据操作类型处理
        switch (op) {
            case EPOLL_CTL_ADD -> instance.addOrUpdate(fd, events); // 新增监控
            case EPOLL_CTL_MOD -> {
                // 修改前确保 fd 已注册
                if (!instance.containsFd(fd)) {
                    throw new IllegalArgumentException("EPOLL_CTL_MOD: fd not registered " + fd);
                }
                instance.addOrUpdate(fd, events);
            }
            case EPOLL_CTL_DEL -> {
                // 删除前确保 fd 已注册
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
