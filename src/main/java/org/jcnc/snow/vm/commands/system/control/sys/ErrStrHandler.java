package org.jcnc.snow.vm.commands.system.control.sys;

import org.jcnc.snow.vm.commands.system.control.SyscallUtils;
import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code ErrStrHandler} 实现对最近一次系统调用错误信息（字符串）的获取。
 * <p>
 * 该处理器会将最近一次系统调用的错误字符串（errstr）压入虚拟机操作数栈顶：
 * <ul>
 *   <li>若无错误，推入空字符串</li>
 *   <li>否则，推入错误消息内容</li>
 * </ul>
 * 错误字符串的获取由 {@link SyscallUtils#getErrStr()} 完成。
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
