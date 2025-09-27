package org.jcnc.snow.vm.commands.system.control.process;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.ThreadRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code ThreadCreateHandler} 实现 THREAD_CREATE (0x1506) 系统调用，
 * 用于在 VM 内部创建新线程并返回其线程 ID。
 *
 * <p><b>Stack：</b>
 * 入参 {@code (entry:any, arg:any)} →
 * 出参 {@code (tid:int)}
 * </p>
 *
 * <p><b>语义：</b>
 * 在虚拟机环境下创建一个新线程。支持两种 entry 形式：
 * <ul>
 *   <li>{@code Runnable}：直接作为线程体执行</li>
 *   <li>{@code String}：通用入口名（如 "app.worker"），模拟为打印 arg 并返回 {@code "done:" + arg}</li>
 * </ul>
 * 子线程的返回值存储于 {@link ThreadRegistry}。
 * </p>
 *
 * <p><b>返回：</b>
 * 成功返回新线程的 tid（int），失败返回 -1。
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>entry 类型不支持时返回 -1，不抛出异常</li>
 *   <li>其它异常理论上被 Runnable 体捕获，返回值通过 ThreadRegistry 提交</li>
 * </ul>
 * </p>
 */
public class ThreadCreateHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack, LocalVariableStore locals, CallStack callStack) {
        // 出栈顺序：先 arg，再 entry
        Object arg = stack.pop();
        Object entry = stack.pop();

        // 组装要执行的 Runnable
        final Runnable task;

        if (entry instanceof Runnable runnable) {
            task = runnable;
        } else if (entry instanceof String name) {
            task = () -> {
                Object result = null;
                try {
                    System.out.println("子线程运行中: " + arg);
                    result = "done:" + arg;
                } finally {
                    ThreadRegistry.setResult(Thread.currentThread().threadId(), result);
                }
            };
        } else {
            System.err.println("THREAD_CREATE: unsupported entry type: "
                    + (entry == null ? "null" : entry.getClass().getName()));
            stack.push(-1);
            return;
        }

        // 启动线程并登记
        Thread thread = new Thread(task, "snow-thread-" + System.nanoTime());
        ThreadRegistry.register(thread);
        thread.start();

        // 返回线程 tid（int）
        long tid = thread.threadId();
        stack.push((int) tid);
    }
}
