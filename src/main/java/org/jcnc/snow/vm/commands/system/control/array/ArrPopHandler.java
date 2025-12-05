package org.jcnc.snow.vm.commands.system.control.array;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.util.List;

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

        Object arrObj = stack.pop();

        if (!(arrObj instanceof List<?> list)) {
            throw new IllegalArgumentException("ARR_POP: not a List: " + arrObj);
        }

        @SuppressWarnings("unchecked")
        List<Object> mlist = (List<Object>) list;

        if (mlist.isEmpty()) {
            throw new IndexOutOfBoundsException("ARR_POP: empty list");
        }

        Object elem = mlist.remove(mlist.size() - 1);

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