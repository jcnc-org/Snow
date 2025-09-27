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
 * 入参 {@code (mid:int)} → 出参 {@code (rc:int)}
 * </p>
 *
 * <p><b>语义：</b>
 * 阻塞直到成功获得指定互斥量 mid 的锁。支持从操作数栈或局部变量表 index=0 获取参数，兼容不同编译器产物。
 * </p>
 *
 * <p><b>返回：</b>
 * 成功返回 {@code 0}。
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>mid 参数类型错误或缺失时抛出 {@link IllegalArgumentException}</li>
 *   <li>mid 无效时抛出异常</li>
 * </ul>
 * </p>
 */
public class MutexLockHandler implements SyscallHandler {
    private static Integer resolveMid(OperandStack stack, LocalVariableStore locals) {
        // 首选：从操作数栈读取
        if (stack != null && !stack.isEmpty()) {
            Object midObj = stack.pop();
            if (midObj instanceof Integer i) {
                return i;
            }
            throw new IllegalArgumentException("MUTEX_LOCK: mid must be int");
        }
        // 兼容路径：从局部变量表 index=0 尝试读取
        if (locals != null) {
            try {
                Object v = locals.getVariable(0);
                if (v instanceof Integer i) {
                    return i;
                }
            } catch (Exception ignore) {
                // fall through
            }
        }
        throw new IllegalArgumentException("MUTEX_LOCK: missing parameter mid");
    }

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        int mid = resolveMid(stack, locals);
        ReentrantLock lock = MutexRegistry.get(mid);
        lock.lock();
        // 约定：成功返回 0
        stack.push(0);
    }
}
