package org.jcnc.snow.vm.commands.system.control.array;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * <p>
 * {@code ArrGetHandler} 是一个系统调用处理器，实现了从数组或列表对象中获取指定索引元素的功能。
 * 本处理器会从操作数栈中弹出索引和数组对象，读取对应位置的元素，并将其重新压入操作数栈。
 * </p>
 * <p>
 * 入栈规则如下：
 * <ul>
 *     <li>若元素为 {@link Number} 类型，则直接入栈。</li>
 *     <li>若元素为 {@link Boolean} 类型，true 入栈 1，false 入栈 0。</li>
 *     <li>其他类型直接入栈。</li>
 * </ul>
 * </p>
 *
 * <p>
 * 处理流程：
 * <ol>
 *     <li>从操作数栈弹出索引（可为数字或字符串）。</li>
 *     <li>再弹出目标数组或列表对象。</li>
 *     <li>根据索引读取元素，并重新压入栈顶。</li>
 * </ol>
 * </p>
 */
public class ArrGetHandler implements SyscallHandler {

    /**
     * 处理数组/列表的元素读取（ARR_GET）。
     * <p>
     * 从操作数栈依次弹出索引和数组对象，根据索引获取元素，并将其入栈。
     * 支持 {@link java.util.List} 及任意 Java 原生数组。
     * </p>
     *
     * @param stack     操作数栈，提供方法参数与返回值
     * @param locals    局部变量存储器（本方法未使用）
     * @param callStack 调用栈（本方法未使用）
     * @throws Exception                如果类型不匹配或索引非法，或发生其他错误时抛出
     * @throws IllegalArgumentException 当传入的对象不是 List 也不是数组时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 从栈顶弹出索引对象
        Object idxObj = stack.pop();
        // 从栈顶弹出数组或列表对象
        Object arrObj = stack.pop();

        // 索引转为 int 类型
        int idx = (idxObj instanceof Number n)
                ? n.intValue()
                : Integer.parseInt(idxObj.toString().trim());

        Object elem;

        // 若为 List，直接获取
        if (arrObj instanceof java.util.List<?> list) {
            elem = list.get(idx);
        }
        // 若为原生数组，反射获取
        else if (arrObj != null && arrObj.getClass().isArray()) {
            elem = java.lang.reflect.Array.get(arrObj, idx);
        }
        // 类型不符，抛出异常
        else {
            throw new IllegalArgumentException("ARR_GET: not an array/list: " + arrObj);
        }

        // 按类型将元素压回栈
        if (elem instanceof Number n) {
            stack.push(n);
        } else if (elem instanceof Boolean b) {
            stack.push(b ? 1 : 0);
        } else {
            stack.push(elem);
        }
    }
}
