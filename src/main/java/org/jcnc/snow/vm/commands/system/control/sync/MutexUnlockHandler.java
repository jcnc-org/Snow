package org.jcnc.snow.vm.commands.system.control.sync;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.MutexRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.util.concurrent.locks.ReentrantLock;

/**
 * {@code MutexUnlockHandler} 实现 MUTEX_UNLOCK (0x1603) 系统调用，
 * 用于解锁指定的互斥量。
 *
 * <p><b>Stack：</b>
 * 入参 {@code (mid:int)} →
 * 出参 {@code (rc:int)}
 * </p>
 *
 * <p><b>语义：</b>
 * 释放由当前线程持有的互斥量（mid）。
 * </p>
 *
 * <p><b>返回：</b>
 * 成功返回 {@code 0}。
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>mid 非 int 或不存在时抛出 {@link IllegalArgumentException}</li>
 *   <li>若当前线程未持有锁，抛出 {@link java.lang.IllegalMonitorStateException}</li>
 * </ul>
 * </p>
 */
public class MutexUnlockHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        Object midObj = stack.pop();
        if (!(midObj instanceof Integer)) {
            throw new IllegalArgumentException("MUTEX_UNLOCK: mid must be int");
        }
        ReentrantLock lock = MutexRegistry.get((Integer) midObj);
        lock.unlock(); // may throw IllegalMonitorStateException if not held
        stack.push(0);
    }
}
