package org.jcnc.snow.vm.commands.system.control.sync;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.SemRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.util.concurrent.Semaphore;

/**
 * {@code SemPostHandler} 实现 SEM_POST (0x160A) 系统调用，
 * 用于释放信号量（post 操作）。
 *
 * <p><b>Stack：</b>
 * 入参 {@code (sid:int)} →
 * 出参 {@code (rc:int)}
 * </p>
 *
 * <p><b>语义：</b>
 * 对指定的信号量（sid）执行 release/post 操作，唤醒等待的线程（如有）。
 * </p>
 *
 * <p><b>返回：</b>
 * 成功返回 {@code 0}。
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>sid 非 int 时抛出 {@link IllegalArgumentException}</li>
 *   <li>sid 无效时抛出 NullPointerException 或同步异常</li>
 * </ul>
 * </p>
 */
public class SemPostHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        Object sidObj = stack.pop();
        if (!(sidObj instanceof Integer)) {
            throw new IllegalArgumentException("SEM_POST: sid must be int");
        }
        Semaphore sem = SemRegistry.get((Integer) sidObj);
        sem.release();
        stack.push(0);
    }
}
