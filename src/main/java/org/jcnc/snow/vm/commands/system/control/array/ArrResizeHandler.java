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
 * {@code ArrResizeHandler} 实现 ARR_RESIZE (0x1814) 系统调用，
 * 用于调整列表长度。
 *
 * <p><b>Stack</b>：入参 {@code (arr:any, newLen:int)} → 出参 {@code (len:int)}</p>
 *
 * <p><b>语义</b>：
 * 将列表长度调整为 {@code newLen}：
 * <ul>
 *   <li>若新长度小于当前长度，则从末尾开始裁剪元素</li>
 *   <li>若新长度大于当前长度，则在末尾补齐 {@code null}</li>
 * </ul>
 * 目前仅支持 {@link java.util.List}。
 * </p>
 *
 * <p><b>返回</b>：调整后的长度（int），等于 {@code newLen}。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>若 {@code newLen} 为负数，抛出 {@link IllegalArgumentException}</li>
 *   <li>若 {@code arr} 不是 {@link java.util.List}，抛出 {@link IllegalArgumentException}</li>
 * </ul>
 * </p>
 */
public class ArrResizeHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 入栈顺序：(arr, newLen) → 栈顶是 newLen
        Value lenV = stack.popValue();
        Value arrV = stack.popValue();

        int newLen = switch (lenV) {
            case IntValue(int i) -> i;
            case org.jcnc.snow.vm.value.ShortValue(short s) -> s;
            case org.jcnc.snow.vm.value.ByteValue(byte b) -> b;
            case org.jcnc.snow.vm.value.LongValue(long l) -> (int) l;
            default -> throw new IllegalArgumentException("ARR_RESIZE: newLen must be int");
        };

        if (newLen < 0) {
            throw new IllegalArgumentException("ARR_RESIZE: negative length: " + newLen);
        }

        if (!(arrV instanceof RefValue(int id))) {
            throw new IllegalArgumentException("ARR_RESIZE: not an array");
        }
        var obj = SnowRuntime.get().heap().get(id);
        if (!(obj instanceof SnowArrayObject arr)) {
            throw new IllegalArgumentException("ARR_RESIZE: not an array");
        }

        arr.resize(newLen);
        stack.pushValue(new IntValue(newLen));
    }
}