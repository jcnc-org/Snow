package org.jcnc.snow.vm.commands.system.control.fd;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * {@code RenameHandler} 用于实现系统调用 RENAME。
 *
 * <p>
 * 功能：将文件或目录从 {@code oldPath} 重命名为 {@code newPath}。
 * 如果目标路径已存在，会被覆盖（与 POSIX 行为一致）。
 * </p>
 *
 * <p>调用约定：</p>
 * <ul>
 *   <li>入参：{@code oldPath:string}, {@code newPath:string}</li>
 *   <li>出参：{@code 0:int}（表示成功）</li>
 * </ul>
 *
 * <p>说明：</p>
 * <ul>
 *   <li>如果 {@code newPath} 已存在，系统会尝试覆盖。</li>
 *   <li>如果源路径或目标路径无效，会抛出异常。</li>
 * </ul>
 *
 * <p>异常：</p>
 * <ul>
 *   <li>如果参数不是字符串，抛出 {@link IllegalArgumentException}</li>
 *   <li>底层 I/O 操作失败时，抛出 {@link java.io.IOException}</li>
 * </ul>
 */
public class RenameHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // 从操作数栈中依次弹出参数：newPath:string（栈顶），oldPath:string（栈底）
        Object newObj = stack.pop();
        Object oldObj = stack.pop();

        // 校验参数类型
        if (!(oldObj instanceof String oldPath)) {
            throw new IllegalArgumentException("RENAME: oldPath must be a String, got: " + oldObj);
        }
        if (!(newObj instanceof String newPath)) {
            throw new IllegalArgumentException("RENAME: newPath must be a String, got: " + newObj);
        }

        // 转换为 Path 对象
        Path source = Paths.get(oldPath);
        Path target = Paths.get(newPath);

        // 执行重命名（覆盖模式）
        Files.move(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        // 成功返回 0
        stack.push(0);
    }
}
