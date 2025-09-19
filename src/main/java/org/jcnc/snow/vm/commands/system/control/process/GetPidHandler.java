package org.jcnc.snow.vm.commands.system.control.process;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code GetPidHandler} 实现 GETPID (0x1104) 系统调用，
 * 用于获取当前虚拟机进程的进程 ID（pid）。
 *
 * <p><b>Stack</b>：无入参 → 出参 {@code (pid:int)}</p>
 *
 * <p><b>语义</b>：返回当前 JVM 进程的 pid。</p>
 *
 * <p><b>返回</b>：成功时返回当前进程 pid（int）。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>如果当前 JVM 版本不支持 {@link ProcessHandle#current()}，可能抛出 {@link UnsupportedOperationException}</li>
 * </ul>
 * </p>
 */
public class GetPidHandler implements SyscallHandler {

    /**
     * 处理 GETPID 调用。
     *
     * @param stack     操作数栈，用于返回当前进程 pid
     * @param locals    局部变量存储器（未使用）
     * @param callStack 调用栈（未使用）
     * @throws Exception 进程 ID 获取失败时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 获取当前 JVM 进程 ID（JDK 9+）
        long pid = ProcessHandle.current().pid();

        // 压回结果（int 类型，如果需要兼容更大 PID 可保留 long 类型）
        stack.push((int) pid);
    }
}
