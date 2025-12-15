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
 * {@code ArrPushHandler} 实现 ARR_PUSH (0x1810) 系统调用，
 * 用于在列表末尾追加一个元素。
 *
 * <p><b>Stack</b>：入参 {@code (arr:any, value:any)} → 出参 {@code (len:int)}</p>
 *
 * <p><b>语义</b>：
 * 在可变序列（目前仅支持 {@link java.util.List}）尾部追加一个元素，
 * 并返回追加后的长度。
 * </p>
 *
 * <p><b>返回</b>：追加后列表长度（int）。</p>
 *
 * <p><b>异常</b>：
 * 若 {@code arr} 不是 {@link java.util.List}，抛出 {@link IllegalArgumentException}。
 * </p>
 */
public class ArrPushHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 入栈顺序：(arr, value) → 栈顶是 value
        Value valueV = stack.popValue();
        Value arrV = stack.popValue();

        if (!(arrV instanceof RefValue(int id))) {
            throw new IllegalArgumentException("ARR_PUSH: not an array");
        }
        var obj = SnowRuntime.get().heap().get(id);
        if (!(obj instanceof SnowArrayObject arr)) {
            throw new IllegalArgumentException("ARR_PUSH: not an array");
        }

        arr.push(valueV);
        stack.pushValue(new IntValue(arr.length()));
    }
}