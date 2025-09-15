package org.jcnc.snow.vm.commands.system.control.fd;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.FDTable;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * {@code WriteHandler} 用于实现系统调用 WRITE。
 *
 * <p>
 * 功能：将字节数组写入指定的虚拟文件描述符（fd）对应的通道。
 * </p>
 *
 * <p>调用约定：</p>
 * <ul>
 *   <li>入参：{@code fd:int}, {@code data:byte[]}</li>
 *   <li>出参：{@code count:int} （实际写入的字节数）</li>
 * </ul>
 *
 * <p>说明：</p>
 * <ul>
 *   <li>如果 fd 无效或不可写，会抛出异常。</li>
 *   <li>写入完成后返回实际写入的字节数。</li>
 * </ul>
 *
 * <p>异常：</p>
 * <ul>
 *   <li>如果 fd 不是整数，抛出 {@link IllegalArgumentException}</li>
 *   <li>如果 data 不是字节数组，抛出 {@link IllegalArgumentException}</li>
 *   <li>如果通道不可写，抛出 {@link IllegalArgumentException}</li>
 *   <li>I/O 操作失败时，抛出 {@link java.io.IOException}</li>
 * </ul>
 */
public class WriteHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // 从操作数栈中弹出参数：先 data:byte[]（栈顶），再 fd:int（栈底）
        Object dataObj = stack.pop();
        Object fdObj = stack.pop();

        // 校验参数类型
        if (!(fdObj instanceof Number)) {
            throw new IllegalArgumentException("WRITE: fd must be an int, got: " + fdObj);
        }
        if (!(dataObj instanceof byte[] data)) {
            throw new IllegalArgumentException("WRITE: data must be a byte[], got: " + dataObj);
        }

        int fd = ((Number) fdObj).intValue();

        // 从 FDTable 获取通道
        var ch = FDTable.get(fd);
        if (!(ch instanceof WritableByteChannel)) {
            throw new IllegalArgumentException("WRITE: fd " + fd + " is not writable");
        }
        WritableByteChannel wch = (WritableByteChannel) ch;

        // 执行写入
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int written = wch.write(buffer);

        // 将写入的字节数压回操作数栈
        stack.push(written);
    }
}
