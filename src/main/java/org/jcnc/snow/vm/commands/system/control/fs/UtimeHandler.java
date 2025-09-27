package org.jcnc.snow.vm.commands.system.control.fs;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;

/**
 * {@code UtimeHandler} 实现 UTIME (0x1103) 系统调用，
 * 用于更新文件或目录的访问时间和修改时间。
 *
 * <p><b>Stack：</b> 入参 {@code (path:String, mtime:long, atime:long)} → 出参 {@code (rc:int)}</p>
 *
 * <p><b>语义：</b> 将指定 {@code path} 的修改时间设为 {@code mtime}，
 * 访问时间设为 {@code atime}。时间戳以自 Epoch 起的毫秒表示。
 * 在部分平台或运行时，可能仅支持修改时间（atime 会被忽略）。</p>
 *
 * <p><b>返回：</b> 成功时返回 {@code 0}。</p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>参数类型不正确时抛出 {@link IllegalArgumentException}</li>
 *   <li>路径不存在时抛出 {@link NoSuchFileException}</li>
 *   <li>平台不支持 {@link BasicFileAttributeView} 时抛出 {@link UnsupportedOperationException}</li>
 *   <li>I/O 失败时抛出 {@link java.io.IOException}</li>
 * </ul>
 * </p>
 */
public class UtimeHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        Object atimeObj = stack.pop();
        Object mtimeObj = stack.pop();
        Object pathObj = stack.pop();

        if (!(pathObj instanceof String pathStr)) {
            throw new IllegalArgumentException("UTIME: path must be a String");
        }
        if (!(mtimeObj instanceof Number) || !(atimeObj instanceof Number)) {
            throw new IllegalArgumentException("UTIME: mtime/atime must be long (ms since epoch)");
        }

        long mtime = ((Number) mtimeObj).longValue();
        long atime = ((Number) atimeObj).longValue();

        Path path = Paths.get(pathStr);
        if (!Files.exists(path)) {
            throw new NoSuchFileException("UTIME: path does not exist: " + pathStr);
        }

        FileTime mtimeFT = FileTime.fromMillis(mtime);
        FileTime atimeFT = FileTime.fromMillis(atime);

        BasicFileAttributeView view =
                Files.getFileAttributeView(path, BasicFileAttributeView.class);

        if (view == null) {
            throw new UnsupportedOperationException("UTIME: BasicFileAttributeView not supported");
        }

        try {
            // 部分平台可能忽略 atime
            view.setTimes(mtimeFT, atimeFT, null);
        } catch (UnsupportedOperationException e) {
            view.setTimes(mtimeFT, null, null);
        }

        stack.push(0);
    }
}
