package org.jcnc.snow.vm.commands.system.control.fd;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * {@code ReadlinkHandler} 用于实现系统调用 READLINK。
 *
 * <p>
 * 功能：读取符号链接 {@code path} 的目标路径，并返回目标字符串。
 * </p>
 *
 * <p>调用约定：</p>
 * <ul>
 *   <li>入参：{@code path:string}</li>
 *   <li>出参：{@code target:string}</li>
 * </ul>
 *
 * <p>说明：</p>
 * <ul>
 *   <li>如果 {@code path} 不是符号链接，会抛出异常。</li>
 *   <li>返回的路径是符号链接指向的目标，可能是相对路径。</li>
 *   <li>不会解析符号链接（即不跟随到最终文件）。</li>
 * </ul>
 *
 * <p>异常：</p>
 * <ul>
 *   <li>如果参数不是字符串，抛出 {@link IllegalArgumentException}</li>
 *   <li>如果路径不存在或不是符号链接，抛出 {@link java.io.IOException}</li>
 * </ul>
 */
public class ReadlinkHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // 从操作数栈中弹出参数：path:string
        Object pathObj = stack.pop();

        // 校验参数类型
        if (!(pathObj instanceof String pathStr)) {
            throw new IllegalArgumentException("READLINK: path must be a String, got: " + pathObj);
        }

        // 转换为 Path 对象
        Path linkPath = Paths.get(pathStr);

        // 读取符号链接的目标路径
        Path target = Files.readSymbolicLink(linkPath);

        // 将目标路径的字符串形式压回栈
        stack.push(target.toString());
    }
}
