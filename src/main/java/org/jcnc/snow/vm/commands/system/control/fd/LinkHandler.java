package org.jcnc.snow.vm.commands.system.control.fd;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * {@code LinkHandler} 用于实现系统调用 LINK。
 *
 * <p>
 * 功能：在文件系统中创建一个硬链接。
 * 将 {@code oldPath} 指向的文件链接到 {@code newPath}。
 * 两者共享同一个 inode，修改其中一个文件会影响另一个。
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
 *   <li>硬链接只能作用于普通文件，不能跨文件系统。</li>
 *   <li>如果 {@code newPath} 已存在，调用会失败并抛出异常。</li>
 *   <li>如果底层文件系统不支持硬链接，也会抛出异常。</li>
 * </ul>
 *
 * <p>异常：</p>
 * <ul>
 *   <li>如果参数不是字符串，抛出 {@link IllegalArgumentException}</li>
 *   <li>如果 {@code oldPath} 不存在，抛出 {@link java.nio.file.NoSuchFileException}</li>
 *   <li>如果 {@code newPath} 已存在，抛出 {@link java.nio.file.FileAlreadyExistsException}</li>
 *   <li>底层 I/O 操作失败时，抛出 {@link java.io.IOException}</li>
 * </ul>
 */
public class LinkHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // 从操作数栈中依次弹出参数：newPath:string（栈顶），oldPath:string（栈底）
        Object newObj = stack.pop();
        Object oldObj = stack.pop();

        // 校验参数类型
        if (!(oldObj instanceof String oldPath)) {
            throw new IllegalArgumentException("LINK: oldPath must be a String, got: " + oldObj);
        }
        if (!(newObj instanceof String newPath)) {
            throw new IllegalArgumentException("LINK: newPath must be a String, got: " + newObj);
        }

        // 转换为 Path 对象
        Path source = Paths.get(oldPath);
        Path target = Paths.get(newPath);

        // 创建硬链接
        Files.createLink(target, source);

        // 成功返回 0
        stack.push(0);
    }
}
