package org.jcnc.snow.vm.commands.system.control.time;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code TickMsHandler} 实现 TICK_MS (0x1703) 系统调用，
 * 用于获取单调时钟的毫秒数（用于计时/测量）。
 *
 * <p><b>Stack</b>：入参 {@code ()} → 出参 {@code (ms:long)}</p>
 *
 * <p><b>语义</b>：返回一个单调时间的毫秒表示（例如基于 {@code System.nanoTime()} 的转换），适合计算时间间隔；该值不是绝对墙钟时间，可能没有固定的参考起点。</p>
 *
 * <p><b>返回</b>：成功返回单调毫秒数 {@code (long)}。</p>
 *
 * <p><b>异常</b>：通常不抛出异常。</p>
 */
public class TickMsHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // 使用单调时钟（System.nanoTime）转换为毫秒，避免受系统时间调整影响
        long ms = System.nanoTime() / 1_000_000L;
        stack.push(ms);
    }
}
