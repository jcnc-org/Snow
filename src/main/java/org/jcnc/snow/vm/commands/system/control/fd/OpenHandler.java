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
 * {@code OpenHandler} 实现虚拟机系统调用 {@code OPEN} 的语义。
 * <p>
 * 用于在 VM 中打开文件、创建文件描述符，并注册到全局 fd 表。
 *
 * <p><b>调用约定：</b></p>
 * <ul>
 *   <li>参数顺序（自底向上）：path (String), flags (int)</li>
 *   <li>参数类型不正确时抛出 {@link IllegalArgumentException}</li>
 *   <li>flags 会进行一致性校验，发现冲突立即抛出异常</li>
 * </ul>
 *
 * <p><b>返回值：</b></p>
 * <ul>
 *   <li>返回新分配的虚拟 fd（int），通过操作数栈顶返回</li>
 * </ul>
 *
 * <p><b>实现流程：</b></p>
 * <ol>
 *   <li>弹出 <code>flags</code> 和 <code>path</code> 参数</li>
 *   <li>校验并转换 <code>flags</code>，生成 Java NIO 的 <code>OpenOption</code> 集合</li>
 *   <li>调用 {@link java.nio.file.Files#newByteChannel(java.nio.file.Path, java.util.Set, java.nio.file.attribute.FileAttribute...) Files.newByteChannel(Path, Set, FileAttribute...)} 打开文件通道</li>
 *   <li>通过 {@link org.jcnc.snow.vm.io.FDTable#register(java.nio.channels.Channel) FDTable.register(Channel)} 分配 fd 并压入栈返回</li>
 * </ol>
 */
public class OpenHandler implements SyscallHandler {

    /**
     * 处理 OPEN 系统调用，打开或创建文件并分配 fd。
     *
     * @param stack     当前虚拟机操作数栈，参数和返回值均通过此栈传递
     * @param locals    当前方法的本地变量表
     * @param callStack 当前调用栈
     * @throws Exception 文件不存在、权限不足、参数错误等原因均可能抛出异常
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // 栈顶依次为：flags、path（与源码参数从左到右相反）
        int flags = (Integer) stack.pop();
        Object pathObj = stack.pop();
        if (!(pathObj instanceof String pathStr)) {
            throw new IllegalArgumentException("OPEN: path must be a String");
        }

        // 校验并转换 flags
        OpenFlags.validate(flags);

        Path path = Paths.get(pathStr);
        SeekableByteChannel ch = Files.newByteChannel(path, OpenFlags.toOpenOptions(flags));

        // 注册并返回 fd
        int fd = FDTable.register(ch);
        stack.push(fd);
    }
}
