package org.jcnc.snow.vm.commands.system.control.time;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.time.Instant;

/**
 * {@code ClockGettimeHandler} 实现 CLOCK_GETTIME (0x1700) 系统调用，
 * 用于获取指定时钟的纳秒时间戳。
 *
 * <p><b>Stack：</b>
 * 入参 {@code (clockId:int)} → 出参 {@code (nanos:long)}
 * </p>
 *
 * <p><b>语义：</b>
 * 根据 {@code clockId} 返回纳秒时间戳:
 * <ul>
 *   <li>{@code 0 (REALTIME)}: 自 Unix 纪元的墙钟时间，单位纳秒</li>
 *   <li>{@code 1 (MONO)}: 单调时钟，适用于测量时间间隔</li>
 * </ul>
 * </p>
 *
 * <p><b>返回：</b>
 * 成功时返回纳秒时间 {@code (long)}。
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>若 {@code clockId} 非法或不被支持，抛出 {@link IllegalArgumentException}</li>
 * </ul>
 * </p>
 */
public class ClockGettimeHandler implements SyscallHandler {
    private static final int REALTIME = 0;
    private static final int MONO = 1;

    /**
     * 处理 CLOCK_GETTIME 调用。
     *
     * @param stack     操作数栈，提供 clockId 参数
     * @param locals    局部变量存储器（未使用）
     * @param callStack 调用栈（未使用）
     * @throws Exception clockId 非法或不被支持时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // 从操作数栈弹出参数 clockId:int（注意 OperandStack 使用 Object）
        Object raw = stack.pop();
        if (raw == null) {
            throw new IllegalArgumentException("clockId is null");
        }
        if (!(raw instanceof Number)) {
            throw new IllegalArgumentException("clockId must be a Number (int), actual type: " + raw.getClass());
        }
        int clockId = ((Number) raw).intValue();

        long nanos;
        switch (clockId) {
            case REALTIME: {
                // Unix epoch 纳秒：seconds * 1_000_000_000 + nanos-of-second
                Instant now = Instant.now();
                nanos = now.getEpochSecond() * 1_000_000_000L + now.getNano();
                break;
            }
            case MONO: {
                // 单调纳秒（相对时间，适用于测量间隔）
                nanos = System.nanoTime();
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported clockId: " + clockId);
        }

        // 将返回值 nanos:long 压回操作数栈（装箱为 Long）
        stack.push(nanos);
    }
}