package org.jcnc.snow.vm.commands.system.control.sys;

import org.jcnc.snow.vm.commands.system.control.SyscallUtils;
import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code ErrnoHandler} 实现对 VM 系统调用 errno 的获取。
 * <p>
 * 该处理器负责将最近一次系统调用的 errno（错误码）压入操作数栈。
 * <ul>
 *   <li>errno 为 0 时表示无错误；非 0 表示最近一次系统调用失败。</li>
 *   <li>本处理器不会自动清除 errno，留给上层或下一次成功的系统调用清空。</li>
 * </ul>
 */
public class ErrnoHandler implements SyscallHandler {

    /**
     * 将最近一次系统调用的 errno 压入操作数栈顶。
     * <p>
     * errno 的获取由 {@link SyscallUtils#getErrno()} 完成。
     *
     * @param stack     当前虚拟机操作数栈
     * @param locals    当前方法的本地变量表
     * @param callStack 当前调用栈
     * @throws Exception 处理过程中出现的异常
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // 将 errno 值压入栈顶，0 表示无错误
        stack.push(SyscallUtils.getErrno());
        // 不自动清空 errno；由上层或下一次成功调用清空
    }
}
