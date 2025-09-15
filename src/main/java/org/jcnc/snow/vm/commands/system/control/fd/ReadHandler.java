package org.jcnc.snow.vm.commands.system.control.fd;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.FDTable;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * {@code ReadHandler} 实现虚拟机的系统调用 {@code READ} 逻辑。
 * <p>
 * 支持从指定虚拟 fd（文件描述符）对应的通道读取数据，并将实际读取到的字节数组返回。
 *
 * <p><b>调用约定：</b></p>
 * <ul>
 *   <li>参数顺序（自底向上）：fd（int），length（int）</li>
 *   <li>返回值：{@code byte[]}，即实际读取到的数据；可能比 length 小，也可能为 0</li>
 * </ul>
 *
 * <p><b>行为说明：</b></p>
 * <ul>
 *   <li>如果读取到 EOF 或 size 小于等于 0，则返回长度为 0 的数组</li>
 *   <li>若 fd 无效或不是可读通道，将抛出 {@link IllegalArgumentException}</li>
 *   <li>读取过程中如遇 I/O 错误，将抛出 {@link java.io.IOException}</li>
 * </ul>
 *
 * <p><b>异常：</b></p>
 * <ul>
 *   <li>fd 不是整数，抛出 {@link IllegalArgumentException}</li>
 *   <li>length 不是整数或小于 0，抛出 {@link IllegalArgumentException}</li>
 *   <li>通道不可读，抛出 {@link IllegalArgumentException}</li>
 *   <li>I/O 操作失败时，抛出 {@link java.io.IOException}</li>
 * </ul>
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
        if (!(ch instanceof ReadableByteChannel)) {
            throw new IllegalArgumentException("READ: fd " + fd + " is not readable");
        }
        ReadableByteChannel rch = (ReadableByteChannel) ch;

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
