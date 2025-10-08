package org.jcnc.snow.vm.commands.system.control.time;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.time.Instant;

/**
 * {@code TimeOfDayHandler} 实现 TIMEOFDAY (0x1702) 系统调用，
 * 用于兼容式获取当前时间（秒 + 微秒 部分）。
 *
 * <p><b>Stack</b>：入参 {@code ()} → 出参 {@code (sec:long, usec:int)}</p>
 *
 * <p><b>语义</b>：返回自 Unix 纪元（1970-01-01T00:00:00Z）以来的秒数（{@code sec}）和当前秒内的微秒部分（{@code usec}, 0..999_999）。</p>
 *
 * <p><b>返回</b>：成功返回两个值：{@code sec:long} 和 {@code usec:int}（注意压栈顺序由实现约定）。</p>
 *
 * <p><b>异常</b>：通常不抛出异常，但实现可能在极特殊环境下抛出运行时异常。</p>
 */
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