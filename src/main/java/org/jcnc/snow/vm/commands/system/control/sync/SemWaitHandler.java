package org.jcnc.snow.vm.commands.system.control.sync;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.SemRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * {@code SemWaitHandler} 实现 SEM_WAIT (0x1609) 系统调用，
 * 用于尝试获取信号量（支持阻塞/带超时）。
 *
 * <p><b>Stack：</b>
 * 入参 {@code (sid:int, timeout_ms:int?)} →
 * 出参 {@code (rc:int)}
 * </p>
 *
 * <p><b>语义：</b>
 * 等待指定信号量 {@code sid}。如提供 {@code timeout_ms} 并大于 0，则在指定超时内尝试获取；
 * 否则阻塞直到获取信号量或线程被中断。
 * </p>
 *
 * <p><b>返回：</b>
 * <ul>
 *   <li>{@code 1} = 成功获取信号量</li>
 *   <li>{@code 0} = 超时未获取</li>
 *   <li>{@code -1} = 等待过程中被中断</li>
 * </ul>
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>sid 非 int 时抛出 {@link IllegalArgumentException}</li>
 *   <li>sid 无效时抛出 NullPointerException 或同步异常</li>
 * </ul>
 * </p>
 */
public class SemWaitHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        Object timeoutObj = stack.pop();
        Object sidObj = stack.pop();
        if (!(sidObj instanceof Integer)) {
            throw new IllegalArgumentException("SEM_WAIT: sid must be int");
        }
        Semaphore sem = SemRegistry.get((Integer) sidObj);

        int result;
        long timeoutMs = (timeoutObj instanceof Integer) ? ((Integer) timeoutObj).longValue() : 0L;

        if (timeoutMs > 0) {
            boolean ok = sem.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);
            result = ok ? 1 : 0;
        } else {
            try {
                sem.acquire();
                result = 1;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                result = -1;
            }
        }
        stack.push(result);
    }
}
