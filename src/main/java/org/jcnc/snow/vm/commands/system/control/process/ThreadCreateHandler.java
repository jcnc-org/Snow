package org.jcnc.snow.vm.commands.system.control.process;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.ThreadRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * THREAD_CREATE(entry:fn/ptr, arg:any) -> tid:int
 *
 * 创建新线程并返回线程 ID。
 */
public class ThreadCreateHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 入参: arg → entry
        Object arg = stack.pop();
        Object entry = stack.pop();

        // 2. 创建线程执行逻辑
        Thread thread = new Thread(() -> {
            Object result = null;
            try {
                // ⚠️ 调用 VM 的函数调用机制
                if (entry instanceof Runnable) {
                    ((Runnable) entry).run();
                } else {
                    System.err.println("Unsupported thread entry type: " + entry);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                ThreadRegistry.setResult(Thread.currentThread().getId(), result);
            }
        });

        // 3. 注册线程
        ThreadRegistry.register(thread);

        // 4. 启动线程
        thread.start();

        // 5. 获取 tid（Java Thread ID）
        long tid = thread.getId();

        // 6. 压回 tid
        stack.push((int) tid);
    }
}
