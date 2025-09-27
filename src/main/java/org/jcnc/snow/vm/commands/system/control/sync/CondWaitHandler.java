package org.jcnc.snow.vm.commands.system.control.sync;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.CondRegistry;
import org.jcnc.snow.vm.io.MutexRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.util.concurrent.locks.ReentrantLock;

/**
 * {@code CondWaitHandler} 实现 COND_WAIT (0x1605) 系统调用，
 * 使线程在指定条件变量上等待，并在等待前释放指定互斥量，唤醒/超时/中断后重新加锁。
 *
 * <p><b>Stack：</b>
 * 入参 {@code (cid:int, mid:int, timeout_ms:int?)} →
 * 出参 {@code (reason:int)}
 * </p>
 *
 * <p><b>语义：</b>
 * 当前线程释放互斥量 {@code mid} 后，在条件变量 {@code cid} 上等待。
 * 等待被 signal/broadcast 唤醒、超时、或中断，之后重新加锁。
 * </p>
 *
 * <p><b>返回：</b>
 * reason（int）：0=signal/broadcast，1=timeout，-1=interrupted
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>cid 或 mid 非 int 时抛出 {@link IllegalArgumentException}</li>
 *   <li>cid 或 mid 无效时抛出 NullPointerException 或同步异常</li>
 * </ul>
 * </p>
 */
public class CondWaitHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        Object timeoutObj = stack.pop(); // may be null
        Object midObj = stack.pop();
        Object cidObj = stack.pop();
        if (!(cidObj instanceof Integer) || !(midObj instanceof Integer)) {
            throw new IllegalArgumentException("COND_WAIT: (cid:int, mid:int, timeout_ms:int?)");
        }
        int cid = (Integer) cidObj;
        int mid = (Integer) midObj;

        Object monitor = CondRegistry.get(cid);
        ReentrantLock lock = MutexRegistry.get(mid);

        long timeoutMs = (timeoutObj instanceof Integer) ? ((Integer) timeoutObj).longValue() : 0L;

        int reason = 0;
        // 释放互斥量，再等待；随后必须重新持有互斥量
        lock.unlock();
        try {
            synchronized (monitor) {
                if (timeoutMs > 0) {
                    long start = System.currentTimeMillis();
                    try {
                        monitor.wait(timeoutMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        reason = -1;
                    }
                    if (reason == 0) {
                        long elapsed = System.currentTimeMillis() - start;
                        if (elapsed >= timeoutMs) {
                            reason = 1; // 超时
                        }
                    }
                } else {
                    try {
                        monitor.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        reason = -1;
                    }
                }
            }
        } finally {
            // 无论如何重新加锁
            lock.lock();
        }
        stack.push(reason);
    }
}
