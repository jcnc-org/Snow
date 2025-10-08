package org.jcnc.snow.vm.commands.system.control.sys;

import org.jcnc.snow.vm.commands.system.control.SyscallUtils;
import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code ErrStrHandler} 实现 ERRSTR (0x1904) 系统调用，
 * 用于获取最近一次系统调用的错误字符串（errstr）。
 *
 * <p><b>Stack</b>：入参 {@code ()} → 出参 {@code (errstr:string)}</p>
 *
 * <p><b>语义</b>：将最近一次系统调用产生的错误信息字符串压入栈顶。</p>
 *
 * <p><b>返回</b>：若最近一次系统调用没有错误，则返回空字符串 {@code ""}；否则返回错误信息文本。</p>
 *
 * <p><b>异常</b>：正常情况下不抛出异常；若底层错误信息读取失败可能抛出运行时异常。</p>
 */
public class ErrStrHandler implements SyscallHandler {

    /**
     * 将最近一次系统调用的错误字符串（errstr）压入操作数栈顶。
     * <p>
     * 若无错误，推入空串。
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
        String msg = SyscallUtils.getErrStr();
        stack.push(msg == null ? "" : msg);
    }
}
