package org.jcnc.snow.vm.commands.system.control.fs;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

/**
 * {@code ChmodHandler} 实现 CHMOD (0x1105) 系统调用，
 * 用于修改路径对应文件/目录的权限。
 *
 * <p><b>Stack</b>：入参 {@code (path:String, mode:int)} → 出参 {@code (rc:int)}</p>
 *
 * <p><b>语义</b>：将 {@code path} 的权限设置为 {@code mode}；在不支持 POSIX 权限的平台上可能退化。</p>
 *
 * <p><b>返回</b>：成功返回 {@code 0}。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>路径不存在/权限不足/模式非法时抛出 {@link java.io.IOException}</li>
 *   <li>参数类型错误时抛出 {@link IllegalArgumentException}</li>
 * </ul>
 * </p>
 */
public class ChmodHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // 从操作数栈取参数 (顺序：mode:int 在栈顶, path:string 在下面)
        Object modeObj = stack.pop();
        Object pathObj = stack.pop();

        if (!(pathObj instanceof String path)) {
            throw new IllegalArgumentException("CHMOD: path must be a String, got " + pathObj);
        }
        if (!(modeObj instanceof Number)) {
            throw new IllegalArgumentException("CHMOD: mode must be an int, got " + modeObj);
        }

        int mode = ((Number) modeObj).intValue();
        Path file = Paths.get(path);

        // 检查是否支持 POSIX 权限
        PosixFileAttributeView view = Files.getFileAttributeView(file, PosixFileAttributeView.class);
        if (view != null) {
            // 转换 mode → POSIX 权限集合
            Set<PosixFilePermission> perms = new HashSet<>();
            if ((mode & 0400) != 0) perms.add(PosixFilePermission.OWNER_READ);
            if ((mode & 0200) != 0) perms.add(PosixFilePermission.OWNER_WRITE);
            if ((mode & 0100) != 0) perms.add(PosixFilePermission.OWNER_EXECUTE);
            if ((mode & 0040) != 0) perms.add(PosixFilePermission.GROUP_READ);
            if ((mode & 0020) != 0) perms.add(PosixFilePermission.GROUP_WRITE);
            if ((mode & 0010) != 0) perms.add(PosixFilePermission.GROUP_EXECUTE);
            if ((mode & 0004) != 0) perms.add(PosixFilePermission.OTHERS_READ);
            if ((mode & 0002) != 0) perms.add(PosixFilePermission.OTHERS_WRITE);
            if ((mode & 0001) != 0) perms.add(PosixFilePermission.OTHERS_EXECUTE);

            Files.setPosixFilePermissions(file, perms);
        } else {
            // 非 POSIX 系统（如 Windows）
            System.err.println("CHMOD: POSIX permissions not supported on this platform, ignoring.");
        }

        // 成功返回 0
        stack.push(0);
    }
}
