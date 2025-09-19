package org.jcnc.snow.vm.commands.system.control.process;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.util.Optional;

/**
 * {@code GetPpidHandler} 实现 GETPPID (0x1105) 系统调用，
 * 用于获取当前虚拟机进程的父进程 ID（ppid）。
 *
 * <p><b>Stack</b>：无入参 → 出参 {@code (ppid:int)}</p>
 *
 * <p><b>语义</b>：返回当前 JVM 进程的父进程 pid，若无父进程则返回 0。</p>
 *
 * <p><b>返回</b>：成功时返回父进程 pid（int）。如不存在父进程，则返回 0。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>如果当前 JVM 版本不支持 {@link ProcessHandle#parent()}，可能抛出 {@link UnsupportedOperationException}</li>
 * </ul>
 * </p>
 */
public class GetPpidHandler implements SyscallHandler {

    /**
     * 处理 GETPPID 调用。
     *
     * @param stack     操作数栈，用于返回父进程 pid
     * @param locals    局部变量存储器（未使用）
     * @param callStack 调用栈（未使用）
     * @throws Exception 获取父进程 ID 失败时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 获取父进程
        Optional<ProcessHandle> parent = ProcessHandle.current().parent();

        // 提取 pid，如果不存在父进程则返回 0
        long ppid = parent.map(ProcessHandle::pid).orElse(0L);

        // 压回结果
        stack.push((int) ppid);
    }
}
