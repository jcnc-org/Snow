package org.jcnc.snow.vm.commands.system.control.process;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code SleepHandler} 实现 SLEEP (0x1106) 系统调用，
 * 用于让当前线程休眠指定的毫秒数。
 *
 * <p><b>Stack</b>：入参 {@code (ms:int)} → 出参 {@code (0:int)}</p>
 *
 * <p><b>语义</b>：当前线程休眠 {@code ms} 毫秒后返回 0。</p>
 *
 * <p><b>返回</b>：始终返回 0。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>如果参数不是 {@code int}，抛出 {@link IllegalArgumentException}</li>
 *   <li>休眠被中断时抛出 {@link InterruptedException}</li>
 * </ul>
 * </p>
 */
public class SleepHandler implements SyscallHandler {

    /**
     * 处理 SLEEP 调用。
     *
     * @param stack     操作数栈，提供休眠时长 ms
     * @param locals    局部变量存储器（未使用）
     * @param callStack 调用栈（未使用）
     * @throws Exception 参数错误或休眠被中断时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 获取入参并校验类型
        Object msObj = stack.pop();
        if (!(msObj instanceof Integer)) {
            throw new IllegalArgumentException("SLEEP: ms 必须为 int 类型");
        }
        int ms = (int) msObj;

        // 执行休眠
        Thread.sleep(ms);

        // 压回返回值 0
        stack.push(0);
    }
}
