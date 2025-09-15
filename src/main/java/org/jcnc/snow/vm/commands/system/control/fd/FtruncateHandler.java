package org.jcnc.snow.vm.commands.system.control.fd;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.FDTable;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.channels.SeekableByteChannel;

/**
 * {@code FtruncateHandler} 用于实现系统调用 FTRUNCATE。
 *
 * <p>
 * 功能：通过文件描述符 {@code fd} 将文件截断到指定大小。
 * 如果文件比指定大小大，多余部分会被丢弃；
 * 如果文件比指定大小小，会在末尾补零字节。
 * </p>
 *
 * <p>调用约定：</p>
 * <ul>
 *   <li>入参：{@code fd:int}, {@code size:long}</li>
 *   <li>出参：{@code 0:int}（表示成功）</li>
 * </ul>
 *
 * <p>说明：</p>
 * <ul>
 *   <li>只能作用于支持随机访问的通道（即 {@link SeekableByteChannel}）。</li>
 *   <li>如果 fd 无效或不是可截断的通道，会抛出异常。</li>
 * </ul>
 *
 * <p>异常：</p>
 * <ul>
 *   <li>如果参数类型错误，抛出 {@link IllegalArgumentException}</li>
 *   <li>如果 fd 无效或不可写，抛出 {@link IllegalArgumentException}</li>
 *   <li>底层 I/O 操作失败时，抛出 {@link java.io.IOException}</li>
 * </ul>
 */
public class FtruncateHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // 从操作数栈中依次弹出参数：size:long（栈顶），fd:int（栈底）
        Object sizeObj = stack.pop();
        Object fdObj = stack.pop();

        // 校验参数类型
        if (!(fdObj instanceof Number)) {
            throw new IllegalArgumentException("FTRUNCATE: fd must be an int, got: " + fdObj);
        }
        if (!(sizeObj instanceof Number)) {
            throw new IllegalArgumentException("FTRUNCATE: size must be a long, got: " + sizeObj);
        }

        int fd = ((Number) fdObj).intValue();
        long size = ((Number) sizeObj).longValue();

        // 获取 fd 对应的通道
        var ch = FDTable.get(fd);
        if (!(ch instanceof SeekableByteChannel)) {
            throw new IllegalArgumentException("FTRUNCATE: fd " + fd + " is not seekable");
        }

        // 截断文件
        ((SeekableByteChannel) ch).truncate(size);

        // FTRUNCATE 成功返回 0
        stack.push(0);
    }
}
