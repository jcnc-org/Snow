package org.jcnc.snow.vm.commands.system.control.console;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.charset.StandardCharsets;

/**
 * {@code StdoutWriteHandler} 用于实现系统调用 STDOUT_WRITE（0x1203）。
 *
 * <p>
 * 由于某些环境下 fd=1 （stdout）经由 FDTable 获取的通道存在缓冲/刷新不同步的问题，
 * 这里直接写入 {@link System#out} 并显式 {@link System#out#flush()}，确保用户能立即看到输出。
 * </p>
 *
 * <p>调用约定（栈）：入参 (data: byte[] | String | Object) → 出参 (written:int)</p>
 * <ul>
 *   <li>String：按 UTF-8 编码为字节写出</li>
 *   <li>byte[]：原样写出</li>
 *   <li>其它对象：调用 toString() 后按 UTF-8 写出；null 视为字符串 "null"</li>
 * </ul>
 */
public class StdoutWriteHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 取出待输出数据
        Object dataObj = stack.pop();

        // 规范化为字节数组
        byte[] data;
        if (dataObj == null) {
            data = "null".getBytes(StandardCharsets.UTF_8);
        } else if (dataObj instanceof byte[] bytes) {
            data = bytes;
        } else {
            data = dataObj.toString().getBytes(StandardCharsets.UTF_8);
        }

        // 直接写 System.out，避免 FDTable 映射中的缓冲未刷新的问题
        System.out.write(data);
        System.out.flush();

        // 返回实际写入的字节数，保持与以往语义一致
        stack.push(data.length);
    }
}
