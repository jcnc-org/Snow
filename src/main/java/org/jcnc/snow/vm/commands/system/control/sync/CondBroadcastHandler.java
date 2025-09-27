package org.jcnc.snow.vm.commands.system.control.sync;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.CondRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code CondBroadcastHandler} 实现 COND_BROADCAST (0x1607) 系统调用，
 * 用于唤醒所有等待在指定条件变量（cid）上的线程。
 *
 * <p><b>Stack：</b>
 * 入参 {@code (cid:int)} →
 * 出参 {@code (rc:int)}
 * </p>
 *
 * <p><b>语义：</b>
 * 对 {@code cid} 标识的条件变量执行 {@code notifyAll}，唤醒所有正在其上等待的线程。
 * 条件变量对象由 {@link CondRegistry} 管理。
 * </p>
 *
 * <p><b>返回：</b>
 * 成功返回 {@code 0}。
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>cid 非 int 时抛出 {@link IllegalArgumentException}</li>
 *   <li>cid 无效时抛出 NullPointerException 或同步异常</li>
 * </ul>
 * </p>
 */
public class CondBroadcastHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        Object cidObj = stack.pop();
        if (!(cidObj instanceof Integer)) {
            throw new IllegalArgumentException("COND_BROADCAST: cid must be int");
        }
        Object monitor = CondRegistry.get((Integer) cidObj);
        synchronized (monitor) {
            monitor.notifyAll();
        }
        stack.push(0);
    }
}
