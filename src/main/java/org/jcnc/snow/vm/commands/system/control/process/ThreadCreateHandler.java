package org.jcnc.snow.vm.commands.system.control.process;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.ThreadRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code ThreadCreateHandler} 实现 THREAD_CREATE (0x1506) 系统调用，
 * 用于在虚拟机中创建一个新线程并返回其线程 ID。
 *
 * <p><b>Stack</b>：入参 {@code (entry:fn/ptr, arg:any)} → 出参 {@code (tid:int)}</p>
 *
 * <p><b>语义</b>：创建新线程并启动，线程入口为 {@code entry}，参数为 {@code arg}，返回新线程的 tid。</p>
 *
 * <p><b>返回</b>：成功时返回新线程的 tid（int）。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>参数类型不支持时抛出 {@link IllegalArgumentException}</li>
 *   <li>线程启动失败时抛出 {@link RuntimeException}</li>
 * </ul>
 * </p>
 */
public class ThreadCreateHandler implements SyscallHandler {

    /**
     * 处理 THREAD_CREATE 调用。
     *
     * @param stack     操作数栈，依次提供线程入口 entry 和参数 arg
     * @param locals    局部变量存储器（未使用）
     * @param callStack 调用栈（未使用）
     * @throws Exception 参数错误或线程创建失败时抛出
     */
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
                // 调用 VM 的函数调用机制
                if (entry instanceof Runnable) {
                    ((Runnable) entry).run();
                } else {
                    System.err.println("Unsupported thread entry type: " + entry);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                ThreadRegistry.setResult(Thread.currentThread().threadId(), null);
            }
        });

        // 3. 注册线程
        ThreadRegistry.register(thread);

        // 4. 启动线程
        thread.start();

        // 5. 获取 tid（Java Thread ID）
        long tid = thread.threadId();

        // 6. 压回 tid
        stack.push((int) tid);
    }
}
