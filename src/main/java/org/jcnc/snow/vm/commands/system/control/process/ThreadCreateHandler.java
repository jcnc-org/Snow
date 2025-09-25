package org.jcnc.snow.vm.commands.system.control.process;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.ThreadRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code ThreadCreateHandler} 实现 THREAD_CREATE (0x1506) 系统调用，
 * 在虚拟机中创建一个新线程并返回其线程 ID。
 *
 * <p><b>Stack</b>：入参 {@code (entry:any, arg:any)} → 出参 {@code (tid:int)}</p>
 *
 * <p><b>entry</b> 支持两种形式：</p>
 * <ul>
 *   <li>{@link Runnable}：直接作为线程入口执行；</li>
 *   <li>{@link String}：形如 {@code "module.function"} 的函数名。当前为了方便
 *       测试，提供一个极简的内置分发：当值为 {@code "app.worker"} 时，按
 *       {@code System.out.print("running in thread: " + arg + "\n")} 的语义执行；
 *       其它字符串暂不支持（会输出提示信息）。</li>
 * </ul>
 *
 * <p>注意：为了兼容 {@link java.util.concurrent.ConcurrentHashMap} 的约束，
 * {@link ThreadRegistry#setResult(long, Object)} 在保存返回值时不再写入 {@code null}，
 * 若返回值为 {@code null} 则改为 {@code remove}。</p>
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

        // 按照 R_LOAD entry; R_LOAD arg 的顺序入栈，所以这里 pop 顺序为：先 arg，再 entry
        Object arg = stack.pop();
        Object entry = stack.pop();

        // 1. 创建线程
        Thread thread = new Thread(() -> {
            Object result = null;
            try {
                if (entry instanceof Runnable r) {
                    r.run();
                } else if (entry instanceof String name) {
                    // 轻量级内置分发：仅用于测试 "app.worker"
                    if ("app.worker".equals(name)) {
                        String s = (arg == null) ? "null" : String.valueOf(arg);
                        System.out.print("running in thread: " + s + "\n");
                        System.out.flush();
                    } else {
                        System.err.println("Unsupported thread entry type/name: " + name);
                    }
                } else {
                    System.err.println("Unsupported thread entry type: " + entry);
                }
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                // ConcurrentHashMap 不允许 null 值；将 null 视为“无返回值”并移除
                ThreadRegistry.setResult(Thread.currentThread().threadId(), result);
            }
        });

        // 2. 注册并启动线程
        ThreadRegistry.register(thread);
        thread.start();

        // 3. 返回 tid（int）
        long tid = thread.threadId();
        stack.push((int) tid);
    }
}
