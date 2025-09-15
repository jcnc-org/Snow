package org.jcnc.snow.vm.commands.system.control.fd;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * {@code TruncateHandler} 用于实现系统调用 TRUNCATE。
 *
 * <p>
 * 功能：将指定文件截断到给定大小。如果文件比指定大小大，多余部分会被丢弃；
 * 如果文件比指定大小小，会在末尾补零字节。
 * </p>
 *
 * <p>调用约定：</p>
 * <ul>
 *   <li>入参：{@code path:string}, {@code length:long}</li>
 *   <li>出参：无</li>
 * </ul>
 *
 * <p>说明：</p>
 * <ul>
 *   <li>会直接修改底层文件。</li>
 *   <li>如果文件不存在，抛出异常。</li>
 *   <li>只能作用于普通文件，不能用于目录或特殊文件。</li>
 * </ul>
 *
 * <p>异常：</p>
 * <ul>
 *   <li>如果 {@code path} 不是字符串或 {@code length} 不是整数，抛出 {@link IllegalArgumentException}</li>
 *   <li>如果文件不存在或不可写，抛出 {@link java.io.IOException}</li>
 * </ul>
 */
public class TruncateHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // 从操作数栈中依次弹出参数：length:long（栈顶），path:string（栈底）
        Object lengthObj = stack.pop();
        Object pathObj = stack.pop();

        // 校验参数类型
        if (!(pathObj instanceof String pathStr)) {
            throw new IllegalArgumentException("TRUNCATE: path must be a String, got: " + pathObj);
        }
        if (!(lengthObj instanceof Number)) {
            throw new IllegalArgumentException("TRUNCATE: length must be a long, got: " + lengthObj);
        }

        long length = ((Number) lengthObj).longValue();
        Path path = Paths.get(pathStr);

        // 打开文件通道（必须可写）
        try (SeekableByteChannel ch = java.nio.file.Files.newByteChannel(
                path,
                java.util.Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE))) {
            // 截断文件到指定长度
            ch.truncate(length);
        }

        // TRUNCATE 无返回值
    }
}
