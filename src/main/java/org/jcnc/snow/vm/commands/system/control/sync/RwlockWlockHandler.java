package org.jcnc.snow.vm.commands.system.control.sync;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.RwlockRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * {@code RwlockWlockHandler} 实现 RWLOCK_WLOCK (0x160D) 系统调用，
 * 用于加锁指定读写锁的写锁。
 *
 * <p><b>Stack：</b>
 * 入参 {@code (rwl:int)} →
 * 出参 {@code (rc:int)}
 * </p>
 *
 * <p><b>语义：</b>
 * 阻塞直到获得指定读写锁的写锁（独占写入）。
 * </p>
 *
 * <p><b>返回：</b>
 * 成功返回 {@code 0}。
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>rwl 非 int 时抛出 {@link IllegalArgumentException}</li>
 *   <li>内部错误时抛出运行时异常</li>
 * </ul>
 * </p>
 */
public class RwlockWlockHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        Object idObj = stack.pop();
        if (!(idObj instanceof Integer)) {
            throw new IllegalArgumentException("RWLOCK_WLOCK: rwl must be int");
        }
        ReentrantReadWriteLock rwl = RwlockRegistry.get((Integer) idObj);
        rwl.writeLock().lock();
        stack.push(0);
    }
}
