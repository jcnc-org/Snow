package org.jcnc.snow.vm.commands.system.control.array;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.lang.reflect.Array;
import java.util.List;

/**
 * {@code ArrLenHandler} 实现 ARR_LEN (0x1801) 系统调用，
 * 用于获取数组、列表、字符串的长度。
 *
 * <p><b>Stack：</b>
 * 入参 {@code (arr:any)} →
 * 出参 {@code (len:int)}
 * </p>
 *
 * <p><b>语义：</b>
 * 返回数组、{@link List}、{@link CharSequence}（如 String）的长度。
 * 支持 null、数组、列表、字符串等常见类型。
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
        Object arrObj = stack.pop();  // 入参：数组/列表/字符串等

        int len;
        if (arrObj == null) {
            len = 0;
        } else if (arrObj instanceof List<?> list) {
            len = list.size();
        } else if (arrObj.getClass().isArray()) {
            len = Array.getLength(arrObj);
        } else if (arrObj instanceof CharSequence s) {
            len = s.length();
        } else {
            throw new IllegalArgumentException("ARR_LEN: not an array/list/string: " + arrObj);
        }

        stack.push(len); // 压回长度（int）
    }
}
