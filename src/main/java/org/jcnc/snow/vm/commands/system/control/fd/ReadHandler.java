package org.jcnc.snow.vm.commands.system.control.fd;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.FDTable;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * {@code ReadHandler} 实现 READ (0x1001) 系统调用，
 * 用于从指定 fd 对应的可读通道读取数据。
 *
 * <p><b>Stack</b>：入参 {@code (fd:int, length:int)} → 出参 {@code (data:byte[])}</p>
 *
 * <p><b>语义</b>：
 * <ul>
 *   <li>若读到 EOF 或 {@code length <= 0}，返回长度为 0 的字节数组</li>
 *   <li>否则返回实际读取的字节数组</li>
 * </ul>
 * </p>
 *
 * <p><b>返回</b>：实际读取的字节数组。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>fd 非法/不可读时抛出 {@link IllegalArgumentException}</li>
 *   <li>length 类型错误或为负时抛出 {@link IllegalArgumentException}</li>
 *   <li>I/O 失败时抛出 {@link java.io.IOException}</li>
 * </ul>
 * </p>
 */
public class ReadHandler implements SyscallHandler {

    /**
     * 处理 READ 系统调用，从指定 fd 对应通道读取数据。
     *
     * @param stack     虚拟机操作数栈，参数和返回值均通过此栈传递
     * @param locals    当前方法的本地变量表
     * @param callStack 当前调用栈
     * @throws Exception 读取出错或参数非法时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        // 栈顶依次为：size、fd（与源码参数顺序相反）
        int size = (Integer) stack.pop();
        int fd = (Integer) stack.pop();

        if (size <= 0) {
            stack.push(new byte[0]);
            return;
        }

        var ch = FDTable.get(fd);
        if (!(ch instanceof ReadableByteChannel rch)) {
            throw new IllegalArgumentException("READ: fd " + fd + " is not readable");
        }

        ByteBuffer buffer = ByteBuffer.allocate(size);
        int bytesRead = rch.read(buffer);
        if (bytesRead <= 0) {
            stack.push(new byte[0]);
            return;
        }

        byte[] data = new byte[bytesRead];
        buffer.flip();
        buffer.get(data);

        stack.push(data);
    }
}
