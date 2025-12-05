package org.jcnc.snow.vm.commands.system.control.syscalls;

import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * 系统调用处理器接口。
 *
 * <p>
 * 所有系统调用均通过实现本接口来处理实际执行逻辑。
 * 系统调用通常由 VM 指令（如 SYS、SYSCALL）触发，参数与返回值均通过操作数栈传递。
 * </p>
 *
 * <p><b>调用规范：</b></p>
 * <ul>
 *     <li>从 {@link OperandStack} 读取输入参数</li>
 *     <li>执行对应系统调用逻辑</li>
 * <li>将返回值（如有）压回操作数栈</li>
 *     <li>可使用 {@link LocalVariableStore} 和 {@link CallStack} 获取调用环境信息</li>
 * </ul>
 *
 * <p><b>异常：</b></p>
 * 实现方可在必要时抛出异常，VM 捕获后可根据策略选用默认错误处理逻辑。
 */
public interface SyscallHandler {

    /**
     * 执行系统调用逻辑。
     *
     * @param stack     当前调用帧的操作数栈，用于读取输入与写回输出
     * @param locals    当前帧的局部变量表
     * @param callStack 整体调用栈，可用于处理跨帧行为
     * @throws Exception 允许实现方抛出任意异常，由 VM 层统一处理
     */
    void handle(OperandStack stack,
                LocalVariableStore locals,
                CallStack callStack) throws Exception;
}