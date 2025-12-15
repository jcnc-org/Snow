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
 * {@code StderrWriteHandler} 实现 STDERR_WRITE (0x1202) 系统调用，
 * 用于向标准错误输出（stderr）写入字符串。
 *
 * <p><b>Stack</b>：入参 {@code (data:Object)} → 出参 {@code (rc:int)}</p>
 *
 * <p><b>语义</b>：将对象转换为字符串（null 输出为 "null"），写入 {@code System.err}；
 * 操作完成后返回 {@code 0}。</p>
 *
 * <p><b>返回</b>：成功返回 {@code 0}。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>写入过程中发生 I/O 错误时抛出 {@link Exception}</li>
 * </ul>
 * </p>
 */
public class StderrWriteHandler implements SyscallHandler {

    /**
     * 处理系统调用 STDERR_WRITE 的具体实现。
     *
     * @param stack     操作数栈，提供输出数据并接收返回值
     * @param locals    局部变量存储器（本方法未使用）
     * @param callStack 调用栈（本方法未使用）
     * @throws Exception 执行过程中发生错误时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        Value data = stack.popValue();
        byte[] bytes = toBytes(data);
        System.err.write(bytes);
        // 确保立即刷新
        System.err.flush();

        // 向栈压入 0，保持栈平衡
        stack.pushValue(new IntValue(0));
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
