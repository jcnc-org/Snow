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
 * {@code StdoutWriteHandler} 用于实现系统调用 STDOUT_WRITE。
 *
 * <p>
 * 功能：将字节数组或字符串数据写入标准输出通道（fd=1），并返回实际写入的字节数。
 * </p>
 *
 * <p>调用约定：</p>
 * <ul>
 *   <li>入参：{@code data:byte[]} 或 {@code String}，待写入的数据（自底向上）</li>
 *   <li>出参：{@code int}，实际写入的字节数</li>
 * </ul>
 *
 * <p>说明：</p>
 * <ul>
 *   <li>若参数为 {@code String}，自动按 UTF-8 编码为字节数组。</li>
 *   <li>若参数为 null，视为长度为 0 的字节数组。</li>
 *   <li>其它对象自动调用 {@code toString()} 并按 UTF-8 编码为字节数组。</li>
 * </ul>
 *
 * <p>异常：</p>
 * <ul>
 *   <li>标准输出通道不可写时，抛出 {@link IllegalStateException}</li>
 *   <li>写操作发生 I/O 错误时，抛出 {@link java.io.IOException}</li>
 * </ul>
 */
public class StdoutWriteHandler implements SyscallHandler {

    /**
     * 处理系统调用 STDOUT_WRITE 的具体实现。
     *
     * @param stack     操作数栈，数据和返回值均通过此栈传递
     * @param locals    局部变量存储器（本方法未使用）
     * @param callStack 调用栈（本方法未使用）
     * @throws Exception 写操作发生错误时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 弹出待写入的数据对象
        Object obj = stack.pop();

        // 按类型转换为字节数组
        byte[] data = switch (obj) {
            case byte[] b -> b;
            case String s -> s.getBytes(StandardCharsets.UTF_8);
            case null -> new byte[0];
            default -> obj.toString().getBytes(StandardCharsets.UTF_8);
        };

        // 获取标准输出通道（fd=1）
        var ch = FDTable.get(1);
        if (!(ch instanceof WritableByteChannel wch)) {
            throw new IllegalStateException("stdout is not a WritableByteChannel");
        }

        // 写入数据，并返回实际写入的字节数
        int written = wch.write(ByteBuffer.wrap(data));
        stack.push(written);
    }
}
