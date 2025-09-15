package org.jcnc.snow.vm.commands.system.control.console;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.io.FDTable;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;

/**
 * {@code StdoutWriteHandler} 实现虚拟机的标准输出（stdout）写操作。
 * <p>
 * 用于将字节数组或字符串数据写入标准输出（fd=1），返回实际写入的字节数。
 *
 * <p><b>调用约定：</b></p>
 * <ul>
 *   <li>参数：<code>data: byte[]</code> 或 <code>String</code>（自底向上）</li>
 *   <li>返回：实际写入的字节数（int）</li>
 * </ul>
 *
 * <p><b>行为说明：</b></p>
 * <ul>
 *   <li>若参数为 <code>String</code>，自动按 UTF-8 编码为字节数组</li>
 *   <li>若为 null，视为长度为 0 的字节数组</li>
 *   <li>其它对象自动调用 <code>toString()</code> 并编码为字节</li>
 * </ul>
 *
 * <p><b>异常：</b></p>
 * <ul>
 *   <li>标准输出通道不可写时，抛出 {@link IllegalStateException}</li>
 *   <li>写操作发生 I/O 错误时，抛出 {@link java.io.IOException}</li>
 * </ul>
 */
public class StdoutWriteHandler implements SyscallHandler {

    /**
     * 将字节数组或字符串写入标准输出，返回实际写入的字节数。
     *
     * @param stack     虚拟机操作数栈，数据和返回值均通过此栈传递
     * @param locals    当前方法的本地变量表
     * @param callStack 当前调用栈
     * @throws Exception 写操作发生错误时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {
        Object obj = stack.pop();
        byte[] data = switch (obj) {
            case byte[] b -> b;
            case String s -> s.getBytes(StandardCharsets.UTF_8);
            case null -> new byte[0];
            default -> obj.toString().getBytes(StandardCharsets.UTF_8);
        };

        var ch = FDTable.get(1);
        if (!(ch instanceof WritableByteChannel wch)) {
            throw new IllegalStateException("stdout is not a WritableByteChannel");
        }
        int written = wch.write(ByteBuffer.wrap(data));
        stack.push(written);
    }
}
