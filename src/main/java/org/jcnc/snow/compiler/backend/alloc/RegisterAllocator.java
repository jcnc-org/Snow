package org.jcnc.snow.compiler.backend.alloc;

import org.jcnc.snow.compiler.ir.core.IRFunction;
import org.jcnc.snow.compiler.ir.core.IRInstruction;
import org.jcnc.snow.compiler.ir.core.IRValue;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.HashMap;
import java.util.Map;

/**
 * 线性扫描寄存器分配器
 * <p>
 * 本类为 IR（中间表示）中的虚拟寄存器分配物理槽号，通常用于后端生成目标代码时确定
 * 各虚拟寄存器实际对应的物理寄存器或栈槽号。采用简单的线性扫描分配策略。
 * </p>
 * <p>
 * 分配过程如下：
 * <ol>
 *   <li>优先为函数参数分配槽号，从 0 开始，按参数顺序递增。</li>
 *   <li>遍历函数体的每条指令，为尚未分配的目标寄存器及其操作数分配新的槽号。</li>
 *   <li>保证每个虚拟寄存器在函数作用域内分配唯一且连续的槽号。</li>
 * </ol>
 * </p>
 */
public final class RegisterAllocator {

    /**
     * 虚拟寄存器到槽号的分配映射表。
     * <p>
     * 键：虚拟寄存器 {@link IRVirtualRegister}；
     * 值：对应分配的槽编号 {@link Integer}。
     * </p>
     */
    private final Map<IRVirtualRegister, Integer> map = new HashMap<>();

    /**
     * 为指定 IR 函数分配所有虚拟寄存器的槽号。
     * <p>
     * 分配顺序说明：
     * <ol>
     *   <li>首先为所有参数分配槽号。</li>
     *   <li>然后线性遍历函数体，为每个指令涉及的虚拟寄存器（目标或操作数）分配槽号，
     *       遇到未分配的寄存器立即分配下一个可用槽号。</li>
     * </ol>
     * 返回的映射不可变，防止外部修改。
     * </p>
     *
     * @param fn 需要进行寄存器分配的 IR 函数对象
     * @return   一个不可变映射，记录所有虚拟寄存器到槽号的分配关系
     */
    public Map<IRVirtualRegister, Integer> allocate(IRFunction fn) {
        int next = 0; // 下一个可分配的槽编号

        // 1. 优先为所有参数分配槽号（顺序与参数列表一致）
        for (IRVirtualRegister param : fn.parameters()) {
            map.put(param, next++);
        }

        // 2. 线性扫描整个函数体，依次为指令涉及的虚拟寄存器分配槽号
        for (IRInstruction inst : fn.body()) {
            // 2.1 若指令有目标寄存器且尚未分配，则分配新槽号
            if (inst.dest() != null && !map.containsKey(inst.dest())) {
                map.put(inst.dest(), next++);
            }
            // 2.2 遍历所有操作数，若为虚拟寄存器且尚未分配，则分配新槽号
            for (IRValue operand : inst.operands()) {
                if (operand instanceof IRVirtualRegister vr && !map.containsKey(vr)) {
                    map.put(vr, next++);
                }
            }
        }

        // 3. 返回不可变映射，防止外部代码误修改分配结果
        return Map.copyOf(map);
    }
}
