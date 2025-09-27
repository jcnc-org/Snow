package org.jcnc.snow.vm.commands.system.control.sync;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.MutexRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.util.concurrent.locks.ReentrantLock;

/**
 * {@code MutexTrylockHandler} 实现 MUTEX_TRYLOCK (0x1602) 系统调用，
 * 尝试以非阻塞方式加锁互斥量。
 *
 * <p><b>Stack：</b> 入参 {@code (mid:int)} → 出参 {@code (ok:int)}</p>
 *
 * <p><b>语义：</b> 尝试获取指定互斥量（mutex）的锁。与阻塞式加锁不同，
 * 本调用立即返回，无论是否加锁成功：
 * <ul>
 *   <li>成功加锁，返回 {@code 1}</li>
 *   <li>未获取到锁（被占用或已递归持有），返回 {@code 0}</li>
 * </ul>
 * </p>
 *
 * <p><b>POSIX 语义补充：</b>
 * 若本线程已递归持有该锁，则视为 busy，不允许递归加锁，始终返回 {@code 0}。</p>
 *
 * <p><b>返回：</b> {@code 1} 表示获取成功，{@code 0} 表示未获取。</p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>未传递合法参数时抛出 {@link IllegalArgumentException}</li>
 *   <li>互斥量未注册时抛出 {@link IllegalStateException}</li>
 * </ul>
 * </p>
 */
public class MutexTrylockHandler implements SyscallHandler {

    /**
     * 从操作数栈或局部变量表解析 mid；
     * 与其它 handler 保持一致的宽松解析策略。
     *
     * @param stack  操作数栈（优先）
     * @param locals 局部变量表（备用）
     * @return 互斥量 ID
     * @throws IllegalArgumentException 参数缺失或类型不合法时
     */
    private static int resolveMid(OperandStack stack, LocalVariableStore locals) {
        if (stack != null && !stack.isEmpty()) {
            Object midObj = stack.pop();
            if (midObj instanceof Integer i) return i;
            throw new IllegalArgumentException("MUTEX_TRYLOCK: mid 必须是 int");
        }
        if (locals != null) {
            try {
                Object v = locals.getVariable(0);
                if (v instanceof Integer i) return i;
            } catch (Exception ignore) {
            }
        }
        throw new IllegalArgumentException("MUTEX_TRYLOCK: 缺少参数 mid");
    }

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // 1. 解析参数
        int mid = resolveMid(stack, locals);

        // 2. 获取互斥量实例
        ReentrantLock lock = MutexRegistry.get(mid);

        // 3. 判断是否已被当前线程递归持有
        if (lock.isHeldByCurrentThread()) {
            stack.push(0); // busy
            return;
        }

        // 4. 非阻塞尝试加锁
        boolean ok = lock.tryLock();
        stack.push(ok ? 1 : 0);
    }
}
