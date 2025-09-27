package org.jcnc.snow.vm.commands.system.control.sync;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.RwlockRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * {@code RwlockUnlockHandler} 实现 RWLOCK_UNLOCK (0x160E) 系统调用，
 * 用于释放当前线程持有的读写锁（优先释放写锁，否则尝试释放读锁）。
 *
 * <p><b>Stack：</b>
 * 入参 {@code (rwl:int)} →
 * 出参 {@code (rc:int)}
 * </p>
 *
 * <p><b>语义：</b>
 * 优先释放指定读写锁的写锁；如果当前线程没有持有写锁，则尝试释放一个读锁。
 * 如果当前线程既未持有写锁也未持有读锁，则抛出异常。
 * </p>
 *
 * <p><b>返回：</b>
 * 成功返回 {@code 0}。
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>rwl 非 int 时抛出 {@link IllegalArgumentException}</li>
 *   <li>当前线程未持有任何锁时抛出 {@link IllegalMonitorStateException}</li>
 * </ul>
 * </p>
 */
public class RwlockUnlockHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        Object idObj = stack.pop();
        if (!(idObj instanceof Integer)) {
            throw new IllegalArgumentException("RWLOCK_UNLOCK: rwl must be int");
        }
        ReentrantReadWriteLock rwl = RwlockRegistry.get((Integer) idObj);

        if (rwl.isWriteLockedByCurrentThread()) {
            rwl.writeLock().unlock();
        } else if (rwl.getReadHoldCount() > 0) {
            rwl.readLock().unlock();
        } else {
            throw new IllegalMonitorStateException("Current thread does not hold read or write lock");
        }
        stack.push(0);
    }
}
