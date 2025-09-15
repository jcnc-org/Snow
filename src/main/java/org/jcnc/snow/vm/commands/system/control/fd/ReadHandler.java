package org.jcnc.snow.vm.commands.system.control.fd;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.FDTable;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * {@code ReadHandler} 用于实现系统调用 READ。
 *
 * <p>
 * 功能：从虚拟文件描述符（fd）对应的通道中读取数据，并将结果返回为字节数组。
 * </p>
 *
 * <p>调用约定：</p>
 * <ul>
 *   <li>入参：{@code fd:int}, {@code length:int}</li>
 *   <li>出参：{@code byte[]} （实际读取到的数据，可能比 length 短）</li>
 * </ul>
 *
 * <p>说明：</p>
 * <ul>
 *   <li>如果读取到 EOF，则返回长度为 0 的数组。</li>
 *   <li>如果 fd 无效或不是可读通道，会抛出异常。</li>
 * </ul>
 *
 * <p>异常：</p>
 * <ul>
 *   <li>如果 fd 不是整数，抛出 {@link IllegalArgumentException}</li>
 *   <li>如果 length 不是整数或小于 0，抛出 {@link IllegalArgumentException}</li>
 *   <li>如果通道不可读，抛出 {@link IllegalArgumentException}</li>
 *   <li>I/O 操作失败时，抛出 {@link java.io.IOException}</li>
 * </ul>
 */
public class ReadHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // 从操作数栈中弹出参数：先 length:int（栈顶），再 fd:int（栈底）
        Object lengthObj = stack.pop();
        Object fdObj = stack.pop();

        // 校验参数类型
        if (!(fdObj instanceof Number)) {
            throw new IllegalArgumentException("READ: fd must be an int, got: " + fdObj);
        }
        if (!(lengthObj instanceof Number)) {
            throw new IllegalArgumentException("READ: length must be an int, got: " + lengthObj);
        }

        int fd = ((Number) fdObj).intValue();
        int length = ((Number) lengthObj).intValue();

        if (length < 0) {
            throw new IllegalArgumentException("READ: length must be >= 0");
        }

        // 从 FDTable 获取通道
        var ch = FDTable.get(fd);
        if (!(ch instanceof ReadableByteChannel rch)) {
            throw new IllegalArgumentException("READ: fd " + fd + " is not readable");
        }

        // 分配缓冲区并读取数据
        ByteBuffer buffer = ByteBuffer.allocate(length);
        int bytesRead = rch.read(buffer);

        // 如果到达 EOF，返回空数组
        if (bytesRead <= 0) {
            stack.push(new byte[0]);
            return;
        }

        // 拷贝实际读取到的数据
        byte[] data = new byte[bytesRead];
        buffer.flip();
        buffer.get(data);

        // 将结果压回操作数栈
        stack.push(data);
    }
}
