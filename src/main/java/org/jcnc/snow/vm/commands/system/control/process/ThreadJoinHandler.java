package org.jcnc.snow.vm.commands.system.control.process;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.ThreadRegistry;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code ThreadJoinHandler} 实现 THREAD_JOIN (0x1202) 系统调用，
 * 用于等待指定线程结束并获取其返回值。
 *
 * <p><b>Stack</b>：入参 {@code (tid:int)} → 出参 {@code (retval:any?)}</p>
 *
 * <p><b>语义</b>：等待指定 tid 的线程执行结束，返回其结果。</p>
 *
 * <p><b>返回</b>：线程执行结果，允许为 null。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>线程 id 不存在时抛出 {@link IllegalArgumentException}</li>
 *   <li>等待过程中被中断时抛出 {@link InterruptedException}</li>
 * </ul>
 * </p>
 */
public class ThreadJoinHandler implements SyscallHandler {

    /**
     * 处理 THREAD_JOIN 调用。
     *
     * @param stack     操作数栈，提供线程 id（tid）
     * @param locals    局部变量存储器（未使用）
     * @param callStack 调用栈（未使用）
     * @throws Exception 线程 id 不存在或等待被中断时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 获取参数
        int tid = (int) stack.pop();

        // 2. 查找线程
        Thread thread = ThreadRegistry.get(tid);
        if (thread == null) {
            throw new IllegalArgumentException("Invalid thread id: " + tid);
        }

        // 3. 等待线程结束
        thread.join();

        // 4. 获取返回值
        Object result = ThreadRegistry.getResult(tid);

        // 5. 注销线程
        ThreadRegistry.unregister(tid);

        // 6. 压回返回值（允许为 null）
        stack.push(result);
    }
}
