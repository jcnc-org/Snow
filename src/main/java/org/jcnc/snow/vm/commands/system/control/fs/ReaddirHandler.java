package org.jcnc.snow.vm.commands.system.control.fs;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code ReaddirHandler} 实现 READDIR (0x1104) 系统调用，
 * 用于列出目录下的直接子项（非递归）。
 *
 * <p><b>Stack：</b> 入参 {@code (path:String)} → 出参 {@code (entries:String[])}</p>
 *
 * <p><b>语义：</b> 返回目录下的文件/子目录名数组。</p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>路径不存在/不是目录 → {@link NotDirectoryException}</li>
 *   <li>权限不足/不可读 → {@link AccessDeniedException}</li>
 *   <li>I/O 错误 → {@link IOException}</li>
 *   <li>参数非法 → {@link IllegalArgumentException}</li>
 * </ul>
 * </p>
 */
public class ReaddirHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // 1. 获取参数
        Object pathObj = stack.pop();
        if (!(pathObj instanceof String pathStr)) {
            throw new IllegalArgumentException("readdir: path 必须是 String");
        }

        Path dir = Paths.get(pathStr);

        if (!Files.exists(dir)) {
            throw new NoSuchFileException("readdir: 路径不存在 -> " + pathStr);
        }
        if (!Files.isDirectory(dir)) {
            throw new NotDirectoryException("readdir: 不是目录 -> " + pathStr);
        }

        try {
            // 2. 遍历目录子项
            List<String> entries = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path entry : stream) {
                    entries.add(entry.getFileName().toString());
                }
            }

            // 3. 转换为数组并推入栈
            stack.push(entries.toArray(new String[0]));
        } catch (IOException e) {
            throw new IOException("readdir: I/O 错误 -> " + pathStr, e);
        }
    }
}
