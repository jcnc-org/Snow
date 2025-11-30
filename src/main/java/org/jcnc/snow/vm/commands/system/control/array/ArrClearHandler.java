package org.jcnc.snow.vm.commands.system.control.array;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.util.List;

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

        Object arrObj = stack.pop();

        if (!(arrObj instanceof List<?> list)) {
            throw new IllegalArgumentException("ARR_CLEAR: not a List: " + arrObj);
        }

        list.clear();

        // 返回长度 0
        stack.push(0);
    }
}