package org.jcnc.snow.vm.commands.system.control.sync;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.MutexRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.util.concurrent.locks.ReentrantLock;

/**
 * {@code MutexTrylockHandler} 实现 MUTEX_TRYLOCK (0x1602) 系统调用，
 * 尝试非阻塞方式加锁互斥量。
 *
 * <p><b>Stack：</b>
 * 入参 {@code (mid:int)} →
 * 出参 {@code (ok:int)}
 * </p>
 *
 * <p><b>语义：</b>
 * 尝试立即获取指定互斥量（mid）的锁，若成功立即返回 {@code 1}，否则返回 {@code 0}。
 * </p>
 *
 * <p><b>返回：</b>
 * 成功加锁返回 {@code 1}，失败返回 {@code 0}。
 * </p>
 *
 * <p><b>异常：</b>
 * mid 非 int 或不存在时抛出 {@link IllegalArgumentException}。
 * </p>
 */
public class MutexTrylockHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack, LocalVariableStore locals, CallStack callStack) throws Exception {
        Object midObj = stack.pop();
        if (!(midObj instanceof Integer)) {
            throw new IllegalArgumentException("MUTEX_TRYLOCK: mid must be int");
        }
        ReentrantLock lock = MutexRegistry.get((Integer) midObj);
        boolean ok = lock.tryLock();
        stack.push(ok ? 1 : 0);
    }
}
