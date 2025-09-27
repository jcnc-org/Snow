package org.jcnc.snow.vm.commands.system.control.array;

import org.jcnc.snow.vm.commands.system.control.syscalls.SyscallHandler;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.util.List;

/**
 * <p>
 * {@code ArrSetHandler} 是一个系统调用处理器，实现对数组或列表对象的元素赋值（设置）功能。
 * 该处理器从操作数栈中弹出值、索引、数组/列表对象，根据索引将指定元素设为指定值。
 * </p>
 * <p>
 * 对于 {@link java.util.List}：
 * <ul>
 *   <li>当目标索引等于当前长度时，会自动 append。</li>
 *   <li>若索引大于长度，会补齐 null 元素直到目标位置。</li>
 *   <li>否则调用 set 方法替换原有元素。</li>
 * </ul>
 * </p>
 *
 * <p>
 * 操作成功后，向操作数栈压入 0 作为返回值。
 * </p>
 *
 * <p>
 * 处理流程：
 * <ol>
 *   <li>从操作数栈弹出赋值内容、索引和目标数组/列表对象</li>
 *   <li>根据容器类型写入目标位置</li>
 *   <li>操作完成后压入返回值 0</li>
 * </ol>
 * </p>
 */
public class ArrSetHandler implements SyscallHandler {

    /**
     * 处理数组/列表的元素写入操作（ARR_SET）。
     * <p>
     * 从操作数栈依次弹出赋值内容、索引和目标数组/列表对象，根据索引将指定元素写入对应位置。
     * 支持 {@link java.util.List} 及原生 Java 数组。
     * </p>
     *
     * @param stack     操作数栈，提供参数和用于返回操作结果
     * @param locals    局部变量存储器（本方法未使用）
     * @param callStack 调用栈（本方法未使用）
     * @throws Exception                如果类型不匹配或索引非法，或执行过程中发生错误
     * @throws IllegalArgumentException 当弹出的对象不是 List 也不是数组时抛出
     */
    @Override
    public void handle(OperandStack stack,
                       LocalVariableStore locals,
                       CallStack callStack) throws Exception {

        // 从栈顶弹出赋值内容、索引、目标数组/列表对象
        Object value = stack.pop();
        Object idxObj = stack.pop();
        Object arrObj = stack.pop();

        // 将索引对象转换为 int 类型
        int idx = (idxObj instanceof Number n)
                ? n.intValue()
                : Integer.parseInt(idxObj.toString().trim());

        // 支持 List 类型容器
        if (arrObj instanceof List<?> list) {
            @SuppressWarnings("unchecked")
            List<Object> mlist = (List<Object>) list;
            // 若目标索引超出当前长度，则补齐 null
            while (mlist.size() < idx) mlist.add(null);
            // 如果等于长度则 append，否则 set 覆盖
            if (idx == mlist.size()) mlist.add(value);
            else mlist.set(idx, value);
        }
        // 支持原生 Java 数组
        else if (arrObj != null && arrObj.getClass().isArray()) {
            java.lang.reflect.Array.set(arrObj, idx, value);
        }
        // 类型不符，抛出异常
        else {
            throw new IllegalArgumentException("ARR_SET: not an array/list: " + arrObj);
        }

        // 操作完成后压入返回值 0
        stack.push(0);
    }
}
