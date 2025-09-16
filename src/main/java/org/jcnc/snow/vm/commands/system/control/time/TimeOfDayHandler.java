package org.jcnc.snow.vm.commands.system.control.time;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.time.Instant;

public class TimeOfDayHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // 获取当前时刻
        Instant now = Instant.now();

        // sec: long -> 秒数（自 Unix 纪元）
        long sec = now.getEpochSecond();

        // usec: int -> 当前秒内的微秒部分（0..999_999）
        int usec = now.getNano() / 1_000; // 将纳秒转换为微秒

        // 按表格约定返回 sec:long, usec:int
        // 这里我们先 push sec (Long)，再 push usec (Integer) —— 因此栈顶为 usec。
        stack.push(sec);
        stack.push(usec);
    }
}
