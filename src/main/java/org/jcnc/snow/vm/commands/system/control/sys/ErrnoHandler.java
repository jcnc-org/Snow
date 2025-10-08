package org.jcnc.snow.vm.commands.system.control.sys;

import org.jcnc.snow.vm.commands.system.control.SyscallUtils;
import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code ErrnoHandler} 实现 ERRNO (0x1905) 系统调用，
 * 用于获取最近一次系统调用的 errno（错误码）。
 *
 * <p><b>Stack</b>：入参 {@code ()} → 出参 {@code (errno:int)}</p>
 *
 * <p><b>语义</b>：将最近一次系统调用对应的整数错误码压入栈顶。</p>
 *
 * <p><b>返回</b>：无错误时返回 {@code 0}；非零值表示具体错误码（语义由上层/平台定义）。</p>
 *
 * <p><b>异常</b>：正常情况下不抛出异常；若读取 errno 失败可能抛出运行时异常。</p>
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
