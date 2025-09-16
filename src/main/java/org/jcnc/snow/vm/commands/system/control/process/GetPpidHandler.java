package org.jcnc.snow.vm.commands.system.control.process;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.util.Optional;

/**
 * GETPPID() -> ppid:int
 *
 * 返回当前进程的父进程 ID。
 */
public class GetPpidHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 获取父进程
        Optional<ProcessHandle> parent = ProcessHandle.current().parent();

        // 2. 提取 pid，如果不存在父进程则返回 0
        long ppid = parent.map(ProcessHandle::pid).orElse(0L);

        // 3. 压回结果
        stack.push((int) ppid);
    }
}
