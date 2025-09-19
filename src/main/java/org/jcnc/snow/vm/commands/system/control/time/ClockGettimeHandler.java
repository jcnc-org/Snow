package org.jcnc.snow.vm.commands.system.control.time;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.time.Instant;

public class ClockGettimeHandler implements SyscallHandler {
    private static final int REALTIME = 0;
    private static final int MONO = 1;

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
