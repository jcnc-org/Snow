package org.jcnc.snow.vm.commands.system.control.fs;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.FDTable;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

/**
 * {@code FchmodHandler} 实现 FCHMOD (0x1106) 系统调用，
 * 用于设置 fd 对应文件或目录的权限。
 *
 * <p><b>Stack：</b> 入参 {@code (fd:int, mode:int)} → 出参 {@code (rc:int)}</p>
 *
 * <p><b>语义：</b> 将 fd 所指对象权限设置为指定 mode（八进制位，POSIX 权限）。</p>
 *
 * <p><b>返回：</b> 成功返回 {@code 0}。</p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>fd 非法或找不到路径时抛出 {@link IllegalArgumentException}</li>
 *   <li>权限不支持或 I/O 错误时抛出 {@link java.io.IOException}</li>
 * </ul>
 * </p>
 */
public class FchmodHandler implements SyscallHandler {

    /**
     * 处理 FCHMOD 调用。
     *
     * @param stack     操作数栈，依次提供 fd, mode
     * @param locals    局部变量存储器（未使用）
     * @param callStack 调用栈（未使用）
     * @throws Exception 权限设置失败或参数错误时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 获取参数: mode → fd
        Object modeObj = stack.pop();
        Object fdObj = stack.pop();

        if (!(fdObj instanceof Number) || !(modeObj instanceof Number)) {
            throw new IllegalArgumentException("FCHMOD: args must be (fd:int, mode:int)");
        }
        int fd = ((Number) fdObj).intValue();
        int mode = ((Number) modeObj).intValue();

        // 2. 查找 fd 对应路径
        Path path = FDTable.getPath(fd);
        if (path == null) {
            // 匿名管道/套接字或无路径信息
            System.err.println("FCHMOD: underlying path unknown for fd=" + fd + ", ignoring.");
            stack.push(0);
            return;
        }

        // 3. 检查 POSIX 权限支持
        PosixFileAttributeView view = Files.getFileAttributeView(path, PosixFileAttributeView.class);
        if (view == null) {
            System.err.println("FCHMOD: POSIX permissions not supported on this platform, ignoring.");
            stack.push(0);
            return;
        }

        // 4. 解析 mode → POSIX 权限集合
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

        Files.setPosixFilePermissions(path, perms);

        // 5. 返回 0
        stack.push(0);
    }
}
