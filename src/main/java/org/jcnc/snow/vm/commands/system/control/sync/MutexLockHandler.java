package org.jcnc.snow.vm.commands.system.control.sync;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.MutexRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.util.concurrent.locks.ReentrantLock;

/**
 * {@code MutexLockHandler} 实现 MUTEX_LOCK (0x1601) 系统调用，
 * 用于加锁指定的互斥量（mid）。
 *
 * <p><b>Stack：</b>
 * 入参 {@code (mid:int)} →
 * 出参 {@code (rc:int)}
 * </p>
 *
 * <p><b>语义：</b>
 * 阻塞当前线程直到成功获取互斥量 {@code mid}。
 * </p>
 *
 * <p><b>返回：</b>
 * 成功返回 {@code 0}。
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>mid 非 int 时抛出 {@link IllegalArgumentException}</li>
 *   <li>mid 无效时抛出 NullPointerException 或同步异常</li>
 * </ul>
 * </p>
 */
public class MutexLockHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        Object midObj = stack.pop();
        if (!(midObj instanceof Integer)) {
            throw new IllegalArgumentException("MUTEX_LOCK: mid must be int");
        }
        ReentrantLock lock = MutexRegistry.get((Integer) midObj);
        lock.lock();
        stack.push(0);
    }
}
