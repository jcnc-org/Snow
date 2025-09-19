package org.jcnc.snow.vm.commands.system.control.fs;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * {@code ChdirHandler} 实现 CHDIR (0x1102) 系统调用，
 * 用于改变 VM 的当前工作目录（CWD）。
 *
 * <p><b>Stack：</b> 入参 {@code (path:String)} → 出参 {@code (0:int)}</p>
 *
 * <p><b>语义：</b> 将虚拟机运行时的 CWD 切换到指定路径，
 * 后续相对路径解析都基于此目录。</p>
 *
 * <p><b>返回：</b> 成功返回 {@code 0}。</p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>路径不存在时抛出 {@link IOException}</li>
 *   <li>路径不是目录时抛出 {@link IOException}</li>
 *   <li>不可访问或 I/O 错误时抛出 {@link IOException}</li>
 *   <li>参数类型非法时抛出 {@link IllegalArgumentException}</li>
 * </ul>
 * </p>
 */
public class ChdirHandler implements SyscallHandler {

    /**
     * VM 内部保存的当前工作目录（缺省为进程启动目录）
     */
    private static Path currentDir = Paths.get("").toAbsolutePath();

    /**
     * 获取 VM 当前工作目录。
     *
     * @return 当前工作目录 Path
     */
    public static Path getCurrentDir() {
        return currentDir;
    }

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // 1. 取参数
        Object pathObj = stack.pop();
        if (!(pathObj instanceof String pathStr)) {
            throw new IllegalArgumentException("chdir: path 必须是 String");
        }

        Path target = Paths.get(pathStr);

        try {
            // 2. 校验
            if (!Files.exists(target)) {
                throw new IOException("chdir: 路径不存在 -> " + pathStr);
            }
            if (!Files.isDirectory(target)) {
                throw new IOException("chdir: 目标不是目录 -> " + pathStr);
            }

            // 3. 切换 CWD（保存绝对路径）
            currentDir = target.toAbsolutePath().normalize();

            // 4. 返回 0
            stack.push(0);
        } catch (IOException e) {
            throw new IOException("chdir: I/O 错误 -> " + pathStr, e);
        }
    }
}
