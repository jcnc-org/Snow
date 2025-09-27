package org.jcnc.snow.vm.commands.system.control.console;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * {@code StdinReadHandler} 用于实现系统调用 STDIN_READ。
 *
 * <p>
 * 功能：从标准输入流（{@link System#in}）读取一行文本，
 * 并将读取到的字符串结果压入操作数栈。
 * </p>
 *
 * <p>调用约定：</p>
 * <ul>
 *   <li>入参：无</li>
 *   <li>出参：{@code line:String}，读取到的字符串，若 EOF 或错误则为 ""</li>
 * </ul>
 *
 * <p>说明：</p>
 * <ul>
 *   <li>若到达输入结束（EOF）或发生异常，压入空字符串 ""。</li>
 *   <li>本操作不会抛出异常，所有异常将由上层捕获处理。</li>
 * </ul>
 *
 * <p>异常：</p>
 * <ul>
 *   <li>执行过程中发生 I/O 错误时，抛出 {@link Exception}</li>
 * </ul>
 */
public class StdinReadHandler implements SyscallHandler {

    /**
     * 用于读取标准输入的缓冲流
     */
    private static final BufferedReader READER =
            new BufferedReader(new InputStreamReader(System.in));

    /**
     * 处理系统调用 STDIN_READ 的具体实现。
     *
     * @param stack     操作数栈，用于存放读取结果
     * @param locals    局部变量存储器（本方法未使用）
     * @param callStack 调用栈（本方法未使用）
     * @throws Exception 执行过程中发生 I/O 错误时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 从标准输入读取一行
        String line = READER.readLine();

        // 若 EOF 或发生异常，压入空字符串
        if (line == null) {
            line = "";
        }

        // 将结果压入栈顶
        stack.push(line);
    }
}
