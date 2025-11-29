package org.jcnc.snow.vm.commands.system.control.array;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.util.List;

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
        Object idxObj = stack.pop();
        Object arrObj = stack.pop();

        int idx = (idxObj instanceof Number n)
                ? n.intValue()
                : Integer.parseInt(idxObj.toString().trim());

        if (!(arrObj instanceof List<?> list)) {
            throw new IllegalArgumentException("ARR_REMOVE: not a List: " + arrObj);
        }

        @SuppressWarnings("unchecked")
        List<Object> mlist = (List<Object>) list;

        Object elem = mlist.remove(idx);

        // 和 ArrGetHandler 保持一致的推栈规则
        if (elem instanceof Number n) {
            stack.push(n);
        } else if (elem instanceof Boolean b) {
            stack.push(b ? 1 : 0);
        } else {
            stack.push(elem);
        }
    }
}