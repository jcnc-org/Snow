package org.jcnc.snow.vm.commands.system.control.array;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;
import org.jcnc.snow.vm.runtime.SnowArrayObject;
import org.jcnc.snow.vm.runtime.SnowRuntime;
import org.jcnc.snow.vm.value.IntValue;
import org.jcnc.snow.vm.value.RefValue;
import org.jcnc.snow.vm.value.Value;

/**
 * {@code ArrSetHandler} 实现 ARR_SET (0x1803) 系统调用，
 * 用于设置数组/列表在指定索引位置的元素为给定值。
 *
 * <p><b>Stack</b>：入参 {@code (arr:any, index:int, value:any)} → 出参 {@code ()}</p>
 *
 * <p><b>语义</b>：设置数组/列表在指定索引位置的元素为给定值。</p>
 *
 * <p><b>支持</b>：{@link java.util.List}、原生 Java 数组。</p>
 *
 * <p><b>限制</b>：不可变类型（如 {@link CharSequence}）不支持写操作。</p>
 *
 * <p><b>返回</b>：无。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>若 {@code arr} 为 {@code null}、索引越界或类型不支持写入，抛出 {@link IndexOutOfBoundsException} 或 {@link IllegalArgumentException}</li>
 * </ul>
 * </p>
 */
public class ArrSetHandler implements SyscallHandler {

    /**
     * 处理数组/列表的元素写入操作（ARR_SET）。
     * <p>
     * 从操作数栈依次弹出赋值内容、索引和目标数组/列表对象，根据索引将指定元素写入对应位置。
     * 支持 {@link java.util.List} 及原生 Java 数组。
     * </p>
     *
     * @param stack     操作数栈，提供参数和用于返回操作结果
     * @param locals    局部变量存储器（本方法未使用）
     * @param callStack 调用栈（本方法未使用）
     * @throws Exception                如果类型不匹配或索引非法，或执行过程中发生错误
     * @throws IllegalArgumentException 当弹出的对象不是 List 也不是数组时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 从栈顶弹出赋值内容、索引、目标数组/列表对象
        Value valueV = stack.popValue();
        Value idxV = stack.popValue();
        Value arrV = stack.popValue();

        int idx = switch (idxV) {
            case IntValue(int i) -> i;
            case org.jcnc.snow.vm.value.ShortValue(short s) -> s;
            case org.jcnc.snow.vm.value.ByteValue(byte b) -> b;
            case org.jcnc.snow.vm.value.LongValue(long l) -> (int) l;
            default -> throw new IllegalArgumentException("ARR_SET: index must be int");
        };

        if (!(arrV instanceof RefValue(int id))) {
            throw new IllegalArgumentException("ARR_SET: not an array");
        }
        var obj = SnowRuntime.get().heap().get(id);
        if (!(obj instanceof SnowArrayObject arr)) {
            throw new IllegalArgumentException("ARR_SET: not an array");
        }

        if (idx < 0) throw new IndexOutOfBoundsException("ARR_SET: negative index");
        if (idx > arr.length()) {
            arr.resize(idx);
        }
        if (idx == arr.length()) {
            arr.push(valueV);
        } else {
            arr.set(idx, valueV);
        }

        // 操作完成后压入返回值 0
        stack.pushValue(new IntValue(0));
    }
}