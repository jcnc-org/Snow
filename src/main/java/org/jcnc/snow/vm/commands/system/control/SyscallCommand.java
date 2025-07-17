package org.jcnc.snow.vm.commands.system.control;

import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.util.Arrays;

/**
 * SyscallCommand ―― 统一的系统调用入口（opcode = 0x0401）。
 *
 * <p>当前支持子命令：
 * <ul>
 *   <li><b>PRINT</b>   —— 打印，不换行</li>
 *   <li><b>PRINTLN</b> —— 打印并换行</li>
 * </ul>
 *
 * <p>用法示例（VM 指令）：</p>
 * <pre>
 *   1025 PRINT "Hello, Snow!"
 *   I_PUSH 42
 *   1025 PRINTLN
 * </pre>
 */
public class SyscallCommand implements Command {

    @Override
    public int execute(String[] parts, int currentPC,
                       OperandStack operandStack,
                       LocalVariableStore localVariableStore,
                       CallStack callStack) {

        if (parts.length < 2)
            throw new IllegalArgumentException("SYSCALL requires a sub-command");

        String subCmd = parts[1].toUpperCase();

        switch (subCmd) {
            case "PRINT":
            case "PRINTLN": {
                boolean newline = subCmd.equals("PRINTLN");
                String output;

                // 指令里直接带字符串常量
                if (parts.length > 2) {
                    output = String.join(" ", Arrays.copyOfRange(parts, 2, parts.length));
                    if (output.length() >= 2 &&
                        output.startsWith("\"") &&
                        output.endsWith("\"")) {
                        output = output.substring(1, output.length() - 1); // 去掉首尾引号
                    }
                }
                // 没带常量，则弹栈打印
                else {
                    Object value = operandStack.pop();
                    output = String.valueOf(value);
                }

                if (newline) System.out.println(output);
                else          System.out.print(output);
                break;
            }

            default:
                throw new UnsupportedOperationException("Unsupported SYSCALL: " + subCmd);
        }

        // 下一条指令
        return currentPC + 1;
    }
}
