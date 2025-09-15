package org.jcnc.snow.vm.commands.system.control;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code SyscallCommand} 实现虚拟机系统调用分发器，负责根据系统调用 opcode 路由到对应的 {@link SyscallHandler} 实现。
 * <p>
 * 用于在虚拟机指令流中处理所有系统调用相关的操作，并统一管理异常处理和错误状态记录。
 *
 * <p><b>工作流程：</b></p>
 * <ol>
 *   <li>从指令参数解析出 syscall opcode（支持 16 进制或 10 进制字符串）</li>
 *   <li>根据 opcode 查找 {@link SyscallHandler}</li>
 *   <li>调用 handler 处理，成功时清除全局 errno/errstr，失败时压入错误并记录异常信息</li>
 * </ol>
 *
 * <p><b>异常管理：</b></p>
 * <ul>
 *   <li>如果指令参数不足，直接压入参数错误并返回</li>
 *   <li>如果 opcode 解析失败，抛出 {@link IllegalArgumentException}</li>
 *   <li>系统调用 handler 抛出异常时，自动通过 {@link SyscallUtils#pushErr} 压入 -1 并记录错误串</li>
 * </ul>
 *
 * <p><b>返回：</b>始终返回下一个指令位置 {@code pc + 1}</p>
 */
public class SyscallCommand implements Command {

    /**
     * 执行系统调用分发。
     *
     * @param parts     指令参数（parts[1] 必须为 syscall opcode，支持 "0x..." 格式）
     * @param pc        当前程序计数器
     * @param stack     虚拟机操作数栈
     * @param locals    当前方法的本地变量表
     * @param callStack 当前调用栈
     * @return 下一个指令位置（pc + 1）
     * @throws IllegalArgumentException opcode 格式非法时抛出
     */
    @Override
    public int execute(String[] parts,
                       int pc,
                       OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) {

        if (parts.length < 2) {
            SyscallUtils.pushErr(stack, new IllegalArgumentException("Missing syscall opcode"));
            return pc + 1;
        }

        int opcode;
        try {
            String token = parts[1].trim();
            if (token.startsWith("0x") || token.startsWith("0X")) {
                opcode = Integer.parseInt(token.substring(2), 16);
            } else {
                opcode = Integer.parseInt(token);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid syscall opcode format: " + parts[1], e);
        }

        SyscallHandler handler = SyscallFactory.getHandler(opcode);

        try {
            handler.handle(stack, locals, callStack);
            // 成功时重置 errno/errstr
            SyscallUtils.clearErr();
        } catch (Exception e) {
            // 失败时压入 -1（int）并记录错误串
            SyscallUtils.pushErr(stack, e);
        }

        return pc + 1;
    }
}
