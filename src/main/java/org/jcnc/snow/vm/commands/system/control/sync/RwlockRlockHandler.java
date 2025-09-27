package org.jcnc.snow.vm.commands.system.control.sync;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.RwlockRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * {@code RwlockRlockHandler} 实现 RWLOCK_RLOCK (0x160C) 系统调用，
 * 阻塞直到获得读写锁的读锁。
 *
 * <p><b>Stack：</b>
 * 入参 {@code (rwl:int)} →
 * 出参 {@code (rc:int)}
 * </p>
 *
 * <p><b>语义：</b>
 * 阻塞直到获得指定读写锁的读锁。
 * </p>
 *
 * <p><b>返回：</b>
 * 成功返回 {@code 0}。
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>rwl 非 int 时抛出 {@link IllegalArgumentException}</li>
 *   <li>rwl 无效时抛出 NullPointerException</li>
 * </ul>
 * </p>
 */
public class RwlockRlockHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        Object idObj = stack.pop();
        if (!(idObj instanceof Integer)) {
            throw new IllegalArgumentException("RWLOCK_RLOCK: rwl must be int");
        }
        ReentrantReadWriteLock rwl = RwlockRegistry.get((Integer) idObj);
        rwl.readLock().lock();
        stack.push(0);
    }
}
