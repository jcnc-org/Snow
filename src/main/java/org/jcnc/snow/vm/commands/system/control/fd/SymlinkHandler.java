package org.jcnc.snow.vm.commands.system.control.fd;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * {@code SymlinkHandler} 用于实现系统调用 SYMLINK。
 *
 * <p>
 * 功能：在文件系统中创建一个符号链接。
 * 将 {@code linkpath} 指向 {@code target}。
 * </p>
 *
 * <p>调用约定：</p>
 * <ul>
 *   <li>入参：{@code target:string}, {@code linkpath:string}</li>
 *   <li>出参：{@code 0:int}（表示成功）</li>
 * </ul>
 *
 * <p>说明：</p>
 * <ul>
 *   <li>符号链接可以指向不存在的文件（悬挂链接）。</li>
 *   <li>如果 {@code linkpath} 已存在，则操作失败。</li>
 *   <li>符号链接本质是特殊文件，并不复制数据。</li>
 * </ul>
 *
 * <p>异常：</p>
 * <ul>
 *   <li>如果参数不是字符串，抛出 {@link IllegalArgumentException}</li>
 *   <li>如果 {@code linkpath} 已存在，抛出 {@link java.nio.file.FileAlreadyExistsException}</li>
 *   <li>底层 I/O 操作失败时，抛出 {@link java.io.IOException}</li>
 * </ul>
 */
public class SymlinkHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // 从操作数栈中依次弹出参数：linkpath:string（栈顶），target:string（栈底）
        Object linkObj = stack.pop();
        Object targetObj = stack.pop();

        // 校验参数类型
        if (!(targetObj instanceof String target)) {
            throw new IllegalArgumentException("SYMLINK: target must be a String, got: " + targetObj);
        }
        if (!(linkObj instanceof String linkpath)) {
            throw new IllegalArgumentException("SYMLINK: linkpath must be a String, got: " + linkObj);
        }

        // 转换为 Path 对象
        Path targetPath = Paths.get(target);
        Path linkPath = Paths.get(linkpath);

        // 创建符号链接
        Files.createSymbolicLink(linkPath, targetPath);

        // 成功返回 0
        stack.push(0);
    }
}
