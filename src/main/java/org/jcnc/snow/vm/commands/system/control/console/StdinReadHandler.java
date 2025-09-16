package org.jcnc.snow.vm.commands.system.control.console;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * {@code StdinReadHandler} 是一个系统调用处理器，
 * 用于实现虚拟机中的标准输入读取功能。
 *
 * <p>该处理器会从 {@link System#in} 读取一行输入，
 * 并将读取到的字符串压入操作数栈。</p>
 */
public class StdinReadHandler implements SyscallHandler {

    private static final BufferedReader READER =
            new BufferedReader(new InputStreamReader(System.in));

    /**
     * 处理标准输入读取操作（STDIN_READ）。
     *
     * <p>从标准输入流读取一行数据，将结果压入操作数栈。
     * 若到达输入结束或发生错误，压入空字符串。</p>
     *
     * @param stack     操作数栈，用于存放读取结果
     * @param locals    局部变量存储器（未使用）
     * @param callStack 调用栈（未使用）
     * @throws Exception 执行过程中发生 I/O 错误时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 从标准输入读取一行
        String line = READER.readLine();

        // 若 EOF 或错误，返回空字符串
        if (line == null) {
            line = "";
        }

        // 将结果压入栈顶
        stack.push(line);
    }
}
