package org.jcnc.snow.vm.commands.system.control.time;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

public class NanosleepHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // 从操作数栈弹出参数 ns:long（OperandStack 使用 Object）
        Object raw = stack.pop();
        if (raw == null) {
            throw new IllegalArgumentException("nanosleep: ns is null");
        }
        if (!(raw instanceof Number)) {
            throw new IllegalArgumentException("nanosleep: ns must be a Number, actual type: " + raw.getClass());
        }

        long ns = ((Number) raw).longValue();
        if (ns < 0) {
            throw new IllegalArgumentException("nanosleep: ns must be non-negative, actual: " + ns);
        }

        if (ns > 0) {
            long millis = ns / 1_000_000L;
            int nanosRemainder = (int) (ns % 1_000_000L); // Thread.sleep的纳秒参数范围为 0..999999

            try {
                // 如果 millis == 0 且 nanosRemainder == 0，则不会阻塞
                Thread.sleep(millis, nanosRemainder);
            } catch (InterruptedException e) {
                // 恢复中断状态并向上抛出，调用者可决定如何处理
                Thread.currentThread().interrupt();
                throw e;
            }
        }

        // 按表格约定返回 0
        stack.push(0);
    }
}
