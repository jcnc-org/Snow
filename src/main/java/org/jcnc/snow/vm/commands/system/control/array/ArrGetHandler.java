package org.jcnc.snow.vm.commands.system.control.array;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * {@code ArrGetHandler} 实现 ARR_GET (0x1802) 系统调用，
 * 用于获取数组/列表在指定索引位置的元素。
 *
 * <p><b>Stack</b>：入参 {@code (arr:any, index:int)} → 出参 {@code (elem:any)}</p>
 *
 * <p><b>语义</b>：获取数组/列表在指定索引位置的元素。</p>
 *
 * <p><b>支持</b>：{@link java.util.List}、原生 Java 数组、{@link CharSequence}。</p>
 *
 * <p><b>返回</b>：对应索引位置的元素；若是 {@link CharSequence}，返回 {@code char} 或其包装形式。</p>
 *
 * <p><b>异常</b>：
 * <ul>
 *   <li>若 {@code arr} 为 {@code null} 或索引越界，抛出 {@link IndexOutOfBoundsException} 或 {@link IllegalArgumentException}</li>
 *   <li>若类型不支持，抛出 {@link IllegalArgumentException}</li>
 * </ul>
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
