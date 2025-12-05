package org.jcnc.snow.vm.commands.system.control.array;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.util.List;

/**
 * {@code ArrInsertHandler} 实现 ARR_INSERT (0x1812) 系统调用，
 * 用于在指定索引位置插入一个元素。
 *
 * <p><b>Stack</b>：入参 {@code (arr:any, index:int, value:any)} → 出参 {@code (len:int)}</p>
 *
 * <p><b>语义</b>：
 * 在列表的指定位置插入一个元素，插入位置及之后的元素整体后移。
 * 目前仅支持 {@link java.util.List}。
 * </p>
 *
 * <p><b>返回</b>：插入后列表长度（int）。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>若 {@code arr} 不是 {@link java.util.List}，抛出 {@link IllegalArgumentException}</li>
 *   <li>若索引不在 [0, size] 范围内，抛出 {@link IndexOutOfBoundsException}</li>
 * </ul>
 * </p>
 */
public class ArrInsertHandler implements SyscallHandler {

    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 入栈顺序：(arr, index, value) → 栈顶依次是 value, index, arr
        Object value = stack.pop();
        Object idxObj = stack.pop();
        Object arrObj = stack.pop();

        int idx = (idxObj instanceof Number n)
                ? n.intValue()
                : Integer.parseInt(idxObj.toString().trim());

        if (!(arrObj instanceof List<?> list)) {
            throw new IllegalArgumentException("ARR_INSERT: not a List: " + arrObj);
        }

        @SuppressWarnings("unchecked")
        List<Object> mlist = (List<Object>) list;

        if (idx < 0 || idx > mlist.size()) {
            throw new IndexOutOfBoundsException(
                    "ARR_INSERT: index " + idx + " out of bounds for length " + mlist.size());
        }

        mlist.add(idx, value);

        // 返回插入后的长度
        stack.push(mlist.size());
    }
}