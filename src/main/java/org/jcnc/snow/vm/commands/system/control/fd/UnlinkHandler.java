package org.jcnc.snow.vm.commands.system.control.fd;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * {@code UnlinkHandler} 用于实现系统调用 UNLINK。
 *
 * <p>
 * 功能：删除指定路径的文件。
 * </p>
 *
 * <p>调用约定：</p>
 * <ul>
 *   <li>入参：{@code path:string}</li>
 *   <li>出参：int（0 表示成功）</li>
 * </ul>
 *
 * <p>说明：</p>
 * <ul>
 *   <li>如果目标是目录，会抛出异常。</li>
 *   <li>如果文件不存在，会抛出 {@link java.nio.file.NoSuchFileException}。</li>
 * </ul>
 *
 * <p>异常：</p>
 * <ul>
 *   <li>如果 path 不是字符串，抛出 {@link IllegalArgumentException}</li>
 *   <li>I/O 操作失败时，抛出 {@link java.io.IOException}</li>
 * </ul>
 */
public class UnlinkHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // 从操作数栈中弹出参数：path:string
        Object pathObj = stack.pop();

        if (!(pathObj instanceof String pathStr)) {
            throw new IllegalArgumentException("UNLINK: path must be a String, got: " + pathObj);
        }

        Path path = Path.of(pathStr);

        // 删除文件（如果是目录会抛出 DirectoryNotEmptyException）
        Files.delete(path);

        // push 返回值：0 表示成功
        stack.push(0);
    }
}
