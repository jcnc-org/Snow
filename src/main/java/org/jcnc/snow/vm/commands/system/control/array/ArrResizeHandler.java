package org.jcnc.snow.vm.commands.system.control.array;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.util.List;

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
        Object lenObj = stack.pop();
        Object arrObj = stack.pop();

        int newLen = (lenObj instanceof Number n)
                ? n.intValue()
                : Integer.parseInt(lenObj.toString().trim());

        if (newLen < 0) {
            throw new IllegalArgumentException("ARR_RESIZE: negative length: " + newLen);
        }

        if (!(arrObj instanceof List<?> list)) {
            throw new IllegalArgumentException("ARR_RESIZE: not a List: " + arrObj);
        }

        @SuppressWarnings("unchecked")
        List<Object> mlist = (List<Object>) list;

        // 缩短
        while (mlist.size() > newLen) {
            mlist.remove(mlist.size() - 1);
        }

        // 变长：补 null
        while (mlist.size() < newLen) {
            mlist.add(null);
        }

        stack.push(newLen);
    }
}
