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
 * {@code ArrGetHandler} 实现 ARR_GET (0x1802) 系统调用，
 * 用于获取数组/列表在指定索引位置的元素。
 *
 * <p><b>Stack</b>：入参 {@code (arr:any, index:int)} → 出参 {@code (elem:any)}</p>
 *
 * <p><b>语义</b>：获取数组/列表在指定索引位置的元素。</p>
 *
 * <p><b>支持</b>：{@link java.util.List}、原生 Java 数组。</p>
 *
 * <p><b>返回</b>：对应索引位置的元素。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>若 {@code arr} 为 {@code null} 或索引越界，抛出 {@link IndexOutOfBoundsException} 或 {@link IllegalArgumentException}</li>
 *   <li>若类型不支持，抛出 {@link IllegalArgumentException}</li>
 * </ul>
 * </p>
 */
public class ArrGetHandler implements SyscallHandler {

    /**
     * 处理数组/列表的元素读取（ARR_GET）。
     * <p>
     * 从操作数栈依次弹出索引和数组对象，根据索引获取元素，并将其入栈。
     * 支持 {@link java.util.List} 及任意 Java 原生数组。
     * </p>
     *
     * @param stack     操作数栈，提供方法参数与返回值
     * @param locals    局部变量存储器（本方法未使用）
     * @param callStack 调用栈（本方法未使用）
     * @throws Exception                如果类型不匹配或索引非法，或发生其他错误时抛出
     * @throws IllegalArgumentException 当传入的对象不是 List 也不是数组时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        Value idxV = stack.popValue();
        Value arrV = stack.popValue();

        int idx = switch (idxV) {
            case IntValue(int i) -> i;
            case org.jcnc.snow.vm.value.ShortValue(short s) -> s;
            case org.jcnc.snow.vm.value.ByteValue(byte b) -> b;
            case org.jcnc.snow.vm.value.LongValue(long l) -> (int) l;
            default -> throw new IllegalArgumentException("ARR_GET: index must be int");
        };

        if (!(arrV instanceof RefValue(int id))) {
            throw new IllegalArgumentException("ARR_GET: not an array");
        }
        var obj = SnowRuntime.get().heap().get(id);
        if (!(obj instanceof SnowArrayObject arr)) {
            throw new IllegalArgumentException("ARR_GET: not an array");
        }

        stack.pushValue(arr.get(idx));
    }
}