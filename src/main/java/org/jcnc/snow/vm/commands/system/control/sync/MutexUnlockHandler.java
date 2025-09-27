package org.jcnc.snow.vm.commands.system.control.sync;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.MutexRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.util.concurrent.locks.ReentrantLock;

/**
 * {@code MutexUnlockHandler} 实现 MUTEX_UNLOCK (0x1603) 系统调用，
 * 用于解锁指定的互斥量（mid）。
 *
 * <p><b>Stack：</b>
 * 入参 {@code (mid:int)} → 出参 {@code (rc:int)}
 * </p>
 *
 * <p><b>语义：</b>
 * 释放当前线程持有的指定互斥量 mid。如果当前线程未持有该锁，将抛出 {@link java.lang.IllegalMonitorStateException}。
 * 参数支持从操作数栈或局部变量表 index=0 获取，兼容不同产物。
 * </p>
 *
 * <p><b>返回：</b>
 * 成功返回 {@code 0}。
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>mid 参数类型错误或缺失时抛出 {@link IllegalArgumentException}</li>
 *   <li>mid 无效或当前线程未持有锁时抛出 {@link java.lang.IllegalMonitorStateException}</li>
 * </ul>
 * </p>
 */
public class MutexUnlockHandler implements SyscallHandler {
    private static Integer resolveMid(OperandStack stack, LocalVariableStore locals) {
        if (stack != null && !stack.isEmpty()) {
            Object midObj = stack.pop();
            if (midObj instanceof Integer i) {
                return i;
            }
            throw new IllegalArgumentException("MUTEX_UNLOCK: mid must be int");
        }
        if (locals != null) {
            try {
                Object v = locals.getVariable(0);
                if (v instanceof Integer i) return i;
            } catch (Exception ignore) {
            }
        }
        throw new IllegalArgumentException("MUTEX_UNLOCK: missing parameter mid");
    }

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        int mid = resolveMid(stack, locals);
        ReentrantLock lock = MutexRegistry.get(mid);
        lock.unlock(); // may throw IllegalMonitorStateException if not held
        // 约定：成功返回 0
        stack.push(0);
    }
}
