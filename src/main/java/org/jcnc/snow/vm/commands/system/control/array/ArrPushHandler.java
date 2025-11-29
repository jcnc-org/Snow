package org.jcnc.snow.vm.commands.system.control.array;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.util.List;

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
        Object value = stack.pop();
        Object arrObj = stack.pop();

        if (!(arrObj instanceof List<?> list)) {
            throw new IllegalArgumentException("ARR_PUSH: not a List: " + arrObj);
        }

        @SuppressWarnings("unchecked")
        List<Object> mlist = (List<Object>) list;

        mlist.add(value);

        // 返回追加后的长度
        stack.push(mlist.size());
    }
}