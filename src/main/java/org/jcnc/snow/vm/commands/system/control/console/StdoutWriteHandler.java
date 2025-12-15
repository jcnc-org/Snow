package org.jcnc.snow.vm.commands.system.control.console;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;
import org.jcnc.snow.vm.runtime.HeapObject;
import org.jcnc.snow.vm.runtime.SnowArrayObject;
import org.jcnc.snow.vm.runtime.SnowBytesObject;
import org.jcnc.snow.vm.runtime.SnowDictObject;
import org.jcnc.snow.vm.runtime.SnowRuntime;
import org.jcnc.snow.vm.runtime.SnowStringObject;
import org.jcnc.snow.vm.runtime.SnowStructObject;
import org.jcnc.snow.vm.value.BoolValue;
import org.jcnc.snow.vm.value.ByteValue;
import org.jcnc.snow.vm.value.DoubleValue;
import org.jcnc.snow.vm.value.FloatValue;
import org.jcnc.snow.vm.value.IntValue;
import org.jcnc.snow.vm.value.LongValue;
import org.jcnc.snow.vm.value.NullValue;
import org.jcnc.snow.vm.value.RefValue;
import org.jcnc.snow.vm.value.ShortValue;
import org.jcnc.snow.vm.value.Value;

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

        if (stack.isEmpty()) throw new IllegalStateException("STDOUT_WRITE: 缺少参数 data");
        byte[] data = toBytes(stack.popValue());

        // 3. 写入 System.out
        System.out.write(data);
        System.out.flush();

        // 4. 返回实际写入字节数
        stack.pushValue(new IntValue(data.length));
    }

    private static byte[] toBytes(Value v) {
        if (v == null) v = Value.NULL;
        return switch (v) {
            case NullValue _ -> "null".getBytes(StandardCharsets.UTF_8);
            case BoolValue(boolean b) -> (b ? "true" : "false").getBytes(StandardCharsets.UTF_8);
            case ByteValue(byte b) -> Byte.toString(b).getBytes(StandardCharsets.UTF_8);
            case ShortValue(short s) -> Short.toString(s).getBytes(StandardCharsets.UTF_8);
            case IntValue(int i) -> Integer.toString(i).getBytes(StandardCharsets.UTF_8);
            case LongValue(long l) -> Long.toString(l).getBytes(StandardCharsets.UTF_8);
            case FloatValue(float f) -> Float.toString(f).getBytes(StandardCharsets.UTF_8);
            case DoubleValue(double d) -> Double.toString(d).getBytes(StandardCharsets.UTF_8);
            case RefValue(int objectId) -> {
                HeapObject obj = SnowRuntime.get().heap().get(objectId);
                yield switch (obj) {
                    case SnowBytesObject b -> b.unsafeBytes();
                    case SnowStringObject s -> s.value().getBytes(StandardCharsets.UTF_8);
                    case SnowArrayObject a -> a.snapshot().toString().getBytes(StandardCharsets.UTF_8);
                    case SnowDictObject d -> d.snapshot().toString().getBytes(StandardCharsets.UTF_8);
                    case SnowStructObject s -> s.snapshot().toString().getBytes(StandardCharsets.UTF_8);
                };
            }
        };
    }
}
