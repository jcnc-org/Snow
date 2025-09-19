package org.jcnc.snow.vm.commands.system.control.process;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code ExitHandler} 实现 EXIT (0x1101) 系统调用，
 * 用于直接结束当前虚拟机进程。
 *
 * <p><b>Stack</b>：入参 {@code (code:int)} → 无返回</p>
 *
 * <p><b>语义</b>：以指定退出码 {@code code} 立即结束整个 JVM 进程。</p>
 *
 * <p><b>返回</b>：无（JVM 直接退出）</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>如果栈顶元素不是 {@code int}，抛出 {@link IllegalArgumentException}</li>
 *   <li>调用 {@link System#exit(int)} 时可能因安全管理器抛出 {@link SecurityException}</li>
 * </ul>
 * </p>
 */
public class ExitHandler implements SyscallHandler {

    /**
     * 处理 EXIT 调用。
     *
     * @param stack     操作数栈，提供退出码参数 code
     * @param locals    局部变量存储器（未使用）
     * @param callStack 调用栈（未使用）
     * @throws Exception 参数错误或系统拒绝退出时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 获取退出码并校验类型
        Object codeObj = stack.pop();
        if (!(codeObj instanceof Integer)) {
            throw new IllegalArgumentException("EXIT: code 必须为 int 类型");
        }
        int code = (int) codeObj;

        // 直接结束整个 JVM 进程
        System.exit(code);
    }
}
