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
 * {@code ArrClearHandler} 实现 ARR_CLEAR (0x1815) 系统调用，
 * 用于清空列表。
 *
 * <p><b>Stack</b>：入参 {@code (arr:any)} → 出参 {@code (len:int)}</p>
 *
 * <p><b>语义</b>：
 * 清空可变序列（{@link java.util.List}），使其长度变为 0。
 * </p>
 *
 * <p><b>返回</b>：清空后的长度（int），恒为 0。</p>
 *
 * <p><b>异常</b>：
 * 若 {@code arr} 不是 {@link java.util.List}，抛出 {@link IllegalArgumentException}。
 * </p>
 */
public class ArrClearHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        Value arrV = stack.popValue();
        if (!(arrV instanceof RefValue(int id))) {
            throw new IllegalArgumentException("ARR_CLEAR: not an array");
        }
        var obj = SnowRuntime.get().heap().get(id);
        if (!(obj instanceof SnowArrayObject arr)) {
            throw new IllegalArgumentException("ARR_CLEAR: not an array");
        }
        arr.clear();
        stack.pushValue(new IntValue(0));
    }
}