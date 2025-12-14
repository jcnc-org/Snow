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
 * <p><b>Stack</b>：入参 {@code ()} → 出参 {@code (pair:any)}</p>
 *
 * <p><b>语义</b>：返回 {@code [sec:long, usec:int]}，其中 sec 为自 Unix 纪元以来的秒数，usec 为当前秒内微秒部分（0..999_999）。</p>
 *
 * <p><b>返回</b>：成功返回一个二元数组（Object[]），元素为 {@code sec} 与 {@code usec}。</p>
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

        // Return a single composite value to keep syscall ABI single-return.
        stack.push(new Object[]{sec, usec});
    }
}
