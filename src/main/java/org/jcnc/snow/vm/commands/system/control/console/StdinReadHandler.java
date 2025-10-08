package org.jcnc.snow.vm.commands.system.control.console;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * {@code StdinReadHandler} 实现 STDIN_READ (0x1200) 系统调用，
 * 用于从标准输入读取一行文本。
 *
 * <p><b>Stack</b>：入参：无 → 出参 {@code (line:String)}</p>
 *
 * <p><b>语义</b>：从 {@code System.in} 读取一行字符串并返回；若到达 EOF 或读取失败，则返回空字符串。</p>
 *
 * <p><b>返回</b>：读取到的字符串。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>I/O 错误时抛出 {@link Exception}</li>
 * </ul>
 * </p>
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
