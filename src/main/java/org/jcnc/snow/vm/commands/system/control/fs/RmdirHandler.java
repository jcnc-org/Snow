package org.jcnc.snow.vm.commands.system.control.fs;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.io.IOException;
import java.nio.file.*;

/**
 * {@code RmdirHandler} 实现 RMDIR (0x1101) 系统调用，
 * 用于在虚拟机内删除空目录。
 *
 * <p><b>Stack：</b> 入参 {@code (path:String)} → 出参 {@code (0:int)}</p>
 *
 * <p><b>语义：</b> 删除 {@code path} 指定的目录；仅当目录为空时成功。
 * 如果目录非空或不是目录，应抛出异常。</p>
 *
 * <p><b>返回：</b> 成功时返回 {@code 0}。</p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>路径不是目录时抛出 {@link IOException}</li>
 *   <li>目录非空时抛出 {@link IOException}</li>
 *   <li>目录不存在时抛出 {@link IOException}</li>
 *   <li>权限不足或 I/O 错误时抛出 {@link IOException}</li>
 *   <li>参数类型非法时抛出 {@link IllegalArgumentException}</li>
 * </ul>
 * </p>
 */
public class RmdirHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // 1. 获取参数
        Object pathObj = stack.pop();
        if (!(pathObj instanceof String pathStr)) {
            throw new IllegalArgumentException("rmdir: path 必须是 String");
        }

        Path path = Paths.get(pathStr);

        try {
            // 2. 检查是否为目录
            if (!Files.isDirectory(path)) {
                throw new IOException("rmdir: 目标不是目录 -> " + pathStr);
            }

            // 3. 检查目录是否为空
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                if (entries.iterator().hasNext()) {
                    throw new IOException("rmdir: 目录非空 -> " + pathStr);
                }
            }

            // 4. 删除目录
            Files.delete(path);

            // 5. 返回 0
            stack.push(0);
        } catch (NoSuchFileException e) {
            throw new IOException("rmdir: 目录不存在 -> " + pathStr, e);
        } catch (IOException e) {
            throw new IOException("rmdir: I/O 错误 -> " + pathStr, e);
        }
    }
}
