package org.jcnc.snow.vm.commands.system.control.fs;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.file.Path;

/**
 * {@code GetcwdHandler} 实现 GETCWD (0x1103) 系统调用，
 * 用于获取 VM 当前工作目录（CWD）。
 *
 * <p><b>Stack：</b> 入参 {@code ()} → 出参 {@code (cwd:String)}</p>
 *
 * <p><b>语义：</b> 返回虚拟机运行时保存的 CWD，用于相对路径解析。</p>
 *
 * <p><b>返回：</b> 当前工作目录的绝对路径字符串。</p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>若 VM 尚未初始化 CWD，则可能抛出 {@link IllegalStateException}</li>
 * </ul>
 * </p>
 */
public class GetcwdHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // 从 ChdirHandler 获取 VM 当前目录
        Path cwd = ChdirHandler.getCurrentDir();
        if (cwd == null) {
            throw new IllegalStateException("getcwd: VM 当前工作目录未初始化");
        }

        // 压入结果
        stack.push(cwd.toString());
    }
}
