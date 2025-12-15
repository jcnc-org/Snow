package org.jcnc.snow.vm.commands.system.control.array;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import org.jcnc.snow.vm.runtime.SnowArrayObject;
import org.jcnc.snow.vm.runtime.SnowRuntime;
import org.jcnc.snow.vm.value.RefValue;
import org.jcnc.snow.vm.value.Value;

/**
 * {@code ArrPopHandler} 实现 ARR_POP (0x1811) 系统调用，
 * 用于移除并返回列表末尾元素。
 *
 * <p><b>Stack</b>：入参 {@code (arr:any)} → 出参 {@code (elem:any)}</p>
 *
 * <p><b>语义</b>：
 * 从可变序列（{@link java.util.List}）尾部弹出一个元素并返回。
 * </p>
 *
 * <p><b>返回</b>：被弹出的元素（any）。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>若 {@code arr} 不是 {@link java.util.List}，抛出 {@link IllegalArgumentException}</li>
 *   <li>若列表为空，抛出 {@link IndexOutOfBoundsException}</li>
 * </ul>
 * </p>
 */
public class ArrPopHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        Value arrV = stack.popValue();
        if (!(arrV instanceof RefValue(int id))) {
            throw new IllegalArgumentException("ARR_POP: not an array");
        }
        var obj = SnowRuntime.get().heap().get(id);
        if (!(obj instanceof SnowArrayObject arr)) {
            throw new IllegalArgumentException("ARR_POP: not an array");
        }
        stack.pushValue(arr.pop());
    }
}