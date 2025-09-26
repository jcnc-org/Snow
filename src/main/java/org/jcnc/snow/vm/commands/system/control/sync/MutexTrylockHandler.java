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
 * 入参 {@code (mid:int)} → 出参 {@code (ok:int)}（成功=1，失败=0）
 * </p>
 *
 * <p><b>语义：</b>
 * 尝试立即获取互斥量（不阻塞）。若获取成功，返回 1，否则返回 0。
 * </p>
 *
 * <p><b>返回：</b>
 * 成功返回 {@code 1}，失败（已被其他线程持有）返回 {@code 0}。
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>mid 参数类型错误或缺失时抛出 {@link IllegalArgumentException}</li>
 *   <li>mid 无效时抛出异常</li>
 * </ul>
 * </p>
 */
public class MutexTrylockHandler implements SyscallHandler {
    private static Integer resolveMid(OperandStack stack, LocalVariableStore locals) {
        if (stack != null && !stack.isEmpty()) {
            Object midObj = stack.pop();
            if (midObj instanceof Integer i) return i;
            throw new IllegalArgumentException("MUTEX_TRYLOCK: mid must be int");
        }
        if (locals != null) {
            try {
                Object v = locals.getVariable(0);
                if (v instanceof Integer i) return i;
            } catch (Exception ignore) {
            }
        }
        throw new IllegalArgumentException("MUTEX_TRYLOCK: missing parameter mid");
    }

    @Override
    public void handle(OperandStack stack, LocalVariableStore locals, CallStack callStack) throws Exception {
        int mid = resolveMid(stack, locals);
        ReentrantLock lock = MutexRegistry.get(mid);
        boolean ok = lock.tryLock();
        stack.push(ok ? 1 : 0);
    }
}
