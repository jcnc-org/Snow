package org.jcnc.snow.vm.commands.system.control.console;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.nio.charset.StandardCharsets;

/**
 * {@code StdoutWriteHandler} 实现 STDOUT_WRITE (0x1201) 系统调用，
 * 将数据写入标准输出（System.out）。
 *
 * <p><b>Stack：</b> 入参 {@code (data:byte[] | String | Object)} → 出参 {@code (written:int)}</p>
 *
 * <p><b>语义：</b>
 * <ul>
 *   <li>参数为 byte[] 时，按原始字节输出</li>
 *   <li>参数为 null 时，输出字符串 "null"</li>
 *   <li>其它类型，调用 {@code toString()} 后以 UTF-8 编码输出</li>
 * </ul>
 * 输出内容直接写到 System.out，不带自动换行。
 * </p>
 *
 * <p><b>返回：</b>
 * <ul>
 *   <li>实际写入的字节数（int）</li>
 * </ul>
 * </p>
 *
 * <p><b>异常：</b>
 * <ul>
 *   <li>操作数栈为空时抛出 {@link IllegalStateException}</li>
 *   <li>I/O 错误时抛出 {@link java.io.IOException}</li>
 * </ul>
 * </p>
 */
public class StdoutWriteHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 1. 取参数，操作数栈不能为空
        if (stack.isEmpty()) {
            throw new IllegalStateException("STDOUT_WRITE: 缺少参数 data");
        }
        Object dataObj = stack.pop();

        // 2. 类型处理
        final byte[] data;
        if (dataObj instanceof byte[] bytes) {
            data = bytes;
        } else {
            // null → "null"；其它 → .toString()
            data = String.valueOf(dataObj).getBytes(StandardCharsets.UTF_8);
        }

        // 3. 写入 System.out
        System.out.write(data);
        System.out.flush();

        // 4. 返回实际写入字节数
        stack.push(data.length);
    }
}
