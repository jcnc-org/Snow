package org.jcnc.snow.vm.commands.system.control.fs;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;

/**
 * {@code MkdirHandler} 实现 MKDIR (0x1100) 系统调用，
 * 用于在虚拟机内创建新目录。
 *
 * <p><b>Stack：</b> 入参 {@code (path:String, mode:int?)} → 出参 {@code (0:int)}</p>
 *
 * <p><b>语义：</b> 创建 path 指定的目录。若 mode 指定且系统支持，按 POSIX 权限创建。</p>
 *
 * <p><b>返回：</b> 成功时返回 0。</p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>目录已存在时抛出 {@link IOException}</li>
 *   <li>父目录不存在时抛出 {@link IOException}</li>
 *   <li>权限错误、I/O 错误时抛出 {@link IOException}</li>
 *   <li>参数类型非法时抛出 {@link IllegalArgumentException}</li>
 * </ul>
 * </p>
 */
public class MkdirHandler implements SyscallHandler {

    /**
     * 处理 MKDIR 调用。
     *
     * @param stack     操作数栈，依次提供 path, mode
     * @param locals    局部变量存储器（未使用）
     * @param callStack 调用栈（未使用）
     * @throws Exception 目录创建失败或参数错误时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // 1. 获取参数: mode → path
        Object modeObj = stack.pop();
        Object pathObj = stack.pop();

        if (!(pathObj instanceof String pathStr)) {
            throw new IllegalArgumentException("mkdir: path 必须是 String");
        }

        Path path = Paths.get(pathStr);

        try {
            // 2. 创建目录，优先尝试设置 POSIX 权限
            if (modeObj instanceof Integer mode) {
                try {
                    FileAttribute<?> attrs =
                            PosixFilePermissions.asFileAttribute(
                                    PosixFilePermissions.fromString(toPosixPerms(mode)));
                    Files.createDirectory(path, attrs);
                } catch (UnsupportedOperationException e) {
                    // 平台不支持 POSIX 权限，降级普通创建
                    Files.createDirectory(path);
                }
            } else {
                Files.createDirectory(path);
            }

            // 3. 返回 0
            stack.push(0);
        } catch (FileAlreadyExistsException e) {
            throw new IOException("mkdir: 目录已存在 -> " + pathStr, e);
        } catch (NoSuchFileException e) {
            throw new IOException("mkdir: 父目录不存在 -> " + pathStr, e);
        } catch (IOException e) {
            throw new IOException("mkdir: I/O 错误 -> " + pathStr, e);
        }
    }

    /**
     * 将 int 模式（如 0755）转换为 POSIX 权限字符串（rwxrwxrwx）。
     * 仅解析低 9 位。
     *
     * @param mode 权限整数
     * @return POSIX 权限字符串
     */
    private String toPosixPerms(int mode) {
        StringBuilder sb = new StringBuilder(9);
        int[] masks = {0400, 0200, 0100, 040, 020, 010, 04, 02, 01};
        char[] chars = {'r', 'w', 'x'};
        for (int i = 0; i < masks.length; i++) {
            sb.append((mode & masks[i]) != 0 ? chars[i % 3] : '-');
        }
        return sb.toString();
    }
}
