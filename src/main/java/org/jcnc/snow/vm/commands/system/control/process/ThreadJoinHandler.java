package org.jcnc.snow.vm.commands.system.control.process;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.ThreadRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * THREAD_JOIN(tid:int) -> retval:any?
 *
 * 等待线程结束并返回其结果。
 */
public class ThreadJoinHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 获取参数
        int tid = (int) stack.pop();

        // 2. 查找线程
        Thread thread = ThreadRegistry.get((long) tid);
        if (thread == null) {
            throw new IllegalArgumentException("Invalid thread id: " + tid);
        }

        // 3. 等待线程结束
        thread.join();

        // 4. 获取返回值
        Object result = ThreadRegistry.getResult((long) tid);

        // 5. 注销线程
        ThreadRegistry.unregister((long) tid);

        // 6. 压回返回值（允许为 null）
        stack.push(result);
    }
}
