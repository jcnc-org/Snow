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
 * {@code ArrInsertHandler} 实现 ARR_INSERT (0x1812) 系统调用，
 * 用于在指定索引位置插入一个元素。
 *
 * <p><b>Stack</b>：入参 {@code (arr:any, index:int, value:any)} → 出参 {@code (len:int)}</p>
 *
 * <p><b>语义</b>：
 * 在列表的指定位置插入一个元素，插入位置及之后的元素整体后移。
 * 目前仅支持 {@link java.util.List}。
 * </p>
 *
 * <p><b>返回</b>：插入后列表长度（int）。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>若 {@code arr} 不是 {@link java.util.List}，抛出 {@link IllegalArgumentException}</li>
 *   <li>若索引不在 [0, size] 范围内，抛出 {@link IndexOutOfBoundsException}</li>
 * </ul>
 * </p>
 */
public class ArrInsertHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 入栈顺序：(arr, index, value) → 栈顶依次是 value, index, arr
        Value valueV = stack.popValue();
        Value idxV = stack.popValue();
        Value arrV = stack.popValue();

        int idx = switch (idxV) {
            case IntValue(int i) -> i;
            case org.jcnc.snow.vm.value.ShortValue(short s) -> s;
            case org.jcnc.snow.vm.value.ByteValue(byte b) -> b;
            case org.jcnc.snow.vm.value.LongValue(long l) -> (int) l;
            default -> throw new IllegalArgumentException("ARR_INSERT: index must be int");
        };

        if (!(arrV instanceof RefValue(int id))) {
            throw new IllegalArgumentException("ARR_INSERT: not an array");
        }
        var obj = SnowRuntime.get().heap().get(id);
        if (!(obj instanceof SnowArrayObject arr)) {
            throw new IllegalArgumentException("ARR_INSERT: not an array");
        }

        if (idx < 0 || idx > arr.length()) {
            throw new IndexOutOfBoundsException(
                    "ARR_INSERT: index " + idx + " out of bounds for length " + arr.length());
        }

        arr.insert(idx, valueV);
        stack.pushValue(new IntValue(arr.length()));
    }
}