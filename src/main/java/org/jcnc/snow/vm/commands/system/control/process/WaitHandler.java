package org.jcnc.snow.vm.commands.system.control.process;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.ProcessRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code WaitHandler} 实现 WAIT (0x1107) 系统调用，
 * 用于等待子进程结束并返回其退出状态码。
 *
 * <p><b>Stack</b>：入参 {@code (pid:int?)} → 出参 {@code (status:int)}</p>
 *
 * <p><b>语义</b>：等待指定 pid 的子进程或任意子进程结束，并返回其退出码。
 * pid 为 null 或 0 时表示等待任意一个子进程。</p>
 *
 * <p><b>返回</b>：子进程退出码（int）。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>指定 pid 的进程不存在时抛出 {@link IllegalArgumentException}</li>
 *   <li>等待过程中被中断时当前线程中断</li>
 * </ul>
 * </p>
 */
public class WaitHandler implements SyscallHandler {

    /**
     * 处理 WAIT 调用。
     *
     * @param stack     操作数栈，提供待等待的子进程 pid（允许为 null/0）
     * @param locals    局部变量存储器（未使用）
     * @param callStack 调用栈（未使用）
     * @throws Exception 进程不存在或等待过程中线程被中断时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 入参：pid 可能为 null
        Object pidObj = stack.pop();
        Integer pid = (pidObj instanceof Integer) ? (Integer) pidObj : null;

        int status = -1;

        if (pid == null || pid == 0) {
            // 2. 等待任意子进程
            for (Process p : ProcessRegistry.all()) {
                try {
                    status = p.waitFor();
                    ProcessRegistry.unregister(p.pid());
                    break;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } else {
            // 3. 等待指定 pid
            Process p = ProcessRegistry.get(pid.longValue());
            if (p == null) {
                throw new IllegalArgumentException("Invalid pid: " + pid);
            }
            try {
                status = p.waitFor();
            } finally {
                ProcessRegistry.unregister(pid.longValue());
            }
        }

        // 4. 压回退出码
        stack.push(status);
    }
}
