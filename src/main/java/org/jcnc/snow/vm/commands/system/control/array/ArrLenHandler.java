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
 * {@code ArrLenHandler} 实现 ARR_LEN (0x1801) 系统调用，
 * 用于获取数组、列表的长度。
 *
 * <p><b>Stack：</b>
 * 入参 {@code (arr:any)} →
 * 出参 {@code (len:int)}
 * </p>
 *
 * <p><b>语义：</b>
 * 返回数组的长度。
 * 支持 null、数组、列表等常见类型。
 * </p>
 *
 * <p><b>返回：</b>
 * int 类型的长度值。若 arr 为 null，返回 0。
 * </p>
 *
 * <p><b>异常：</b>
 * 非法类型时抛出 {@link IllegalArgumentException}。
 * </p>
 */
public class ArrLenHandler implements SyscallHandler {
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) {
        Value arrV = stack.popValue();
        if (arrV == Value.NULL) {
            stack.pushValue(new IntValue(0));
            return;
        }
        if (!(arrV instanceof RefValue(int id))) {
            throw new IllegalArgumentException("ARR_LEN: not an array");
        }
        var obj = SnowRuntime.get().heap().get(id);
        if (!(obj instanceof SnowArrayObject arr)) {
            throw new IllegalArgumentException("ARR_LEN: not an array");
        }
        stack.pushValue(new IntValue(arr.length()));
    }
}