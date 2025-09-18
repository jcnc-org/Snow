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
 * {@code ChmodHandler} 用于实现系统调用 CHMOD。
 *
 * <p>
 * 功能：修改文件或目录的权限位（mode）。
 * 在支持 POSIX 权限的平台（Linux/macOS）上会应用真实权限；
 * 在不支持的平台（如 Windows）上会忽略该操作，但返回成功。
 * </p>
 *
 * <p>调用约定：</p>
 * <ul>
 *   <li>入参：{@code path:string}, {@code mode:int}</li>
 *   <li>出参：int，成功返回 0</li>
 * </ul>
 *
 * <p>说明：</p>
 * <ul>
 *   <li>POSIX 平台：将 {@code mode} 转换为 {@link PosixFilePermission} 集合并应用。</li>
 *   <li>非 POSIX 平台：直接忽略操作（权限不会改变）。</li>
 *   <li>{@code mode} 使用八进制整数表示，例如 {@code 0755 → 493}。</li>
 * </ul>
 *
 * <p>异常：</p>
 * <ul>
 *   <li>如果 {@code path} 不是字符串或 {@code mode} 不是整数 → {@link IllegalArgumentException}</li>
 *   <li>如果文件不存在或不可访问 → {@link java.io.IOException}</li>
 *   <li>如果底层文件系统不支持 POSIX 权限 → 自动忽略（不抛异常）。</li>
 * </ul>
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
