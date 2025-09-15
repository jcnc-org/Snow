package org.jcnc.snow.vm.commands.system.control.fd;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.FDTable;
import org.jcnc.snow.vm.io.OpenFlags;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * {@code OpenHandler} 用于实现系统调用 OPEN。
 *
 * <p>
 * 功能：根据传入的路径和标志位打开一个文件，并在虚拟机的
 * {@link FDTable} 中注册对应的 {@link SeekableByteChannel}，
 * 最终返回一个虚拟文件描述符（fd）。
 * </p>
 *
 * <p>调用约定：</p>
 * <ul>
 *   <li>入参：{@code path:string}, {@code flags:int}</li>
 *   <li>出参：{@code fd:int}</li>
 * </ul>
 *
 * <p>flags 说明：</p>
 * <p>
 * flags 采用 POSIX 风格定义（见 {@link OpenFlags}），
 * 会被映射为 Java NIO 的 {@link java.nio.file.StandardOpenOption}。
 * 支持的典型组合：
 * </p>
 * <ul>
 *   <li>{@code O_RDONLY}：只读</li>
 *   <li>{@code O_WRONLY | O_CREAT | O_TRUNC}：覆盖写（类似 "w" 模式）</li>
 *   <li>{@code O_WRONLY | O_CREAT | O_APPEND}：追加写（类似 "a" 模式）</li>
 *   <li>{@code O_RDWR | O_CREAT | O_EXCL}：读写，且文件必须不存在</li>
 * </ul>
 *
 * <p>异常：</p>
 * <ul>
 *   <li>如果 path 不是字符串，抛出 {@link IllegalArgumentException}</li>
 *   <li>如果 flags 不是整数，抛出 {@link IllegalArgumentException}</li>
 *   <li>底层 I/O 操作失败时，抛出 {@link java.io.IOException}</li>
 * </ul>
 */

public class OpenHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // 从操作数栈中弹出参数：先 flags:int（栈顶），再 path:string（栈底）
        Object flagsObj = stack.pop();
        Object pathObj = stack.pop();

        // 校验 path 参数类型，必须是字符串
        if (!(pathObj instanceof String pathStr)) {
            throw new IllegalArgumentException("OPEN: path must be a String, got: " + pathObj);
        }
        // 校验 flags 参数类型，必须是整数
        if (!(flagsObj instanceof Number)) {
            throw new IllegalArgumentException("OPEN: flags must be an int, got: " + flagsObj);
        }

        // 将 flags 转换为 int
        int flags = ((Number) flagsObj).intValue();

        // 校验 flags 组合是否合法（例如 O_TRUNC 不能和只读一起用）
        OpenFlags.validate(flags);
        // 根据传入的 path 字符串生成 Path 对象
        Path path = Paths.get(pathStr);

        // 使用 Files.newByteChannel 打开文件通道，
        // 并根据 flags 转换出来的 OpenOption 集合指定打开方式
        SeekableByteChannel ch = Files.newByteChannel(path, OpenFlags.toOpenOptions(flags));

        // 将通道注册到 FDTable，得到一个新的虚拟 fd
        int fd = FDTable.register(ch);
        // 把 fd 压回操作数栈，作为返回值交给调用方
        stack.push(fd);
    }
}
