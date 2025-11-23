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
 * {@code OpenHandler} 实现 OPEN (0x1000) 系统调用，
 * 用于打开文件并返回一个新的 fd。
 *
 * <p><b>Stack</b>：入参 {@code (path:String, flags:int)} → 出参 {@code (fd:int)}</p>
 *
 * <p><b>语义</b>：依据 {@code flags}（由 {@code OpenFlags} 解析为 {@code OpenOption} 集）打开
 * {@code path} 对应的文件，底层通过 {@code Files.newByteChannel(...)} 创建通道并注册到 {@code FDTable}。</p>
 *
 * <p><b>返回</b>：成功时返回新分配的 fd（int）。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>路径/flags 类型错误时抛出 {@link IllegalArgumentException}</li>
 *   <li>flags 非法时抛出 {@link IllegalArgumentException}</li>
 *   <li>文件不存在且未指定创建、权限不足或底层 I/O 失败时抛出 {@link java.io.IOException}</li>
 * </ul>
 * </p>
 */
public class OpenHandler implements SyscallHandler {

    /**
     * 处理 OPEN 系统调用。
     *
     * @param stack     操作数栈，依次提供 path, flags
     * @param locals    局部变量存储器（未使用）
     * @param callStack 调用栈（未使用）
     * @throws Exception 打开文件失败或参数不合法时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // 1. 获取参数: flags → path
        Object flagsObj = stack.pop();
        Object pathObj = stack.pop();

        int flags = (flagsObj instanceof Number) ? ((Number) flagsObj).intValue() : 0;
        String pathStr = (pathObj instanceof String) ? (String) pathObj : null;
        if (pathStr == null) {
            throw new IllegalArgumentException("OPEN: path must be a String");
        }

        // 2. 校验 flags
        OpenFlags.validate(flags);

        // 3. 打开文件通道
        Path path = Paths.get(pathStr);
        SeekableByteChannel ch = Files.newByteChannel(path, OpenFlags.toOpenOptions(flags));

        // 4. 注册并返回 fd
        int fd = FDTable.register(ch, path);
        stack.push(fd);
    }
}
