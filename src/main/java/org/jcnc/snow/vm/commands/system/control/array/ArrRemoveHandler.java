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
 * {@code ArrRemoveHandler} 实现 ARR_REMOVE (0x1813) 系统调用，
 * 用于移除并返回指定索引位置的元素。
 *
 * <p><b>Stack</b>：入参 {@code (arr:any, index:int)} → 出参 {@code (elem:any)}</p>
 *
 * <p><b>语义</b>：
 * 从列表中移除指定索引位置的元素，并返回该元素。
 * 目前仅支持 {@link java.util.List}。
 * </p>
 *
 * <p><b>返回</b>：被移除的元素（any）。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>若 {@code arr} 不是 {@link java.util.List}，抛出 {@link IllegalArgumentException}</li>
 *   <li>若索引越界，由 {@link java.util.List#remove(int)} 抛出 {@link IndexOutOfBoundsException}</li>
 * </ul>
 * </p>
 */
public class ArrRemoveHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 入栈顺序：(arr, index) → 栈顶是 index
        Value idxV = stack.popValue();
        Value arrV = stack.popValue();

        int idx = switch (idxV) {
            case IntValue(int i) -> i;
            case org.jcnc.snow.vm.value.ShortValue(short s) -> s;
            case org.jcnc.snow.vm.value.ByteValue(byte b) -> b;
            case org.jcnc.snow.vm.value.LongValue(long l) -> (int) l;
            default -> throw new IllegalArgumentException("ARR_REMOVE: index must be int");
        };

        if (!(arrV instanceof RefValue(int id))) {
            throw new IllegalArgumentException("ARR_REMOVE: not an array");
        }
        var obj = SnowRuntime.get().heap().get(id);
        if (!(obj instanceof SnowArrayObject arr)) {
            throw new IllegalArgumentException("ARR_REMOVE: not an array");
        }

        stack.pushValue(arr.remove(idx));
    }
}