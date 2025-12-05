package org.jcnc.snow.compiler.backend.alloc;

import org.jcnc.snow.common.GlobalSlot;
import org.jcnc.snow.compiler.ir.common.GlobalVariableTable;
import org.jcnc.snow.compiler.ir.common.GlobalVariableTable.GlobalVariable;
import org.jcnc.snow.compiler.ir.core.IRFunction;
import org.jcnc.snow.compiler.ir.core.IRInstruction;
import org.jcnc.snow.compiler.ir.core.IRValue;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.HashMap;
import java.util.Map;

/**
 * 线性扫描虚拟寄存器分配器。
 * <p>
 * 该类负责在中间表示（IR）阶段，将函数体内所有虚拟寄存器映射为唯一的物理槽编号（通常代表物理寄存器号或栈槽号）。
 * 分配策略采用简单的线性扫描，确保每个虚拟寄存器在当前函数作用域内分配到唯一且连续的槽号，且优先满足参数顺序。
 * </p>
 *
 * <p>
 * <strong>核心分配流程：</strong>
 * <ol>
 *   <li>首先为全局变量注册槽号（全局槽区）。</li>
 *   <li>再为所有函数参数顺序分配槽号（局部槽区）。</li>
 *   <li>线性遍历函数体，为所有尚未分配的虚拟寄存器分配下一个可用槽号，避免与全局区冲突。</li>
 * </ol>
 * </p>
 *
 * <p>
 * <strong>线程安全说明：</strong> 本类实例仅用于单次分配，不保证线程安全。
 * </p>
 *
 * @author （你的名字）
 * @version 1.0
 * @since 2024-12-05
 */
public final class RegisterAllocator {

    /**
     * 虚拟寄存器到物理槽编号的映射表。
     * <p>
     * 键为 {@link IRVirtualRegister}，值为分配的物理槽编号（整型）。
     * </p>
     */
    private final Map<IRVirtualRegister, Integer> map = new HashMap<>();

    /**
     * 为指定 IR 函数分配所有虚拟寄存器的物理槽号。
     *
     * <p>
     * 分配顺序与规则如下：
     * <ol>
     *   <li>首先为所有全局变量预分配全局槽号，避免冲突。</li>
     *   <li>按照参数顺序依次为所有函数参数分配槽号，从0开始连续分配。</li>
     *   <li>线性遍历所有指令，对每个未分配槽号的目标寄存器及操作数分配下一个可用槽号，
     *       且确保不落入全局槽区且唯一。</li>
     * </ol>
     * </p>
     *
     * <p>
     * <b>注意：</b>返回结果为不可变映射，外部不可修改；同一 RegisterAllocator 实例重复调用会复用内部 map。
     * </p>
     *
     * @param fn 需要进行寄存器分配的 IRFunction 函数对象，不能为空
     * @return 虚拟寄存器到物理槽号的不可变映射表，键为 {@link IRVirtualRegister}，值为 {@link Integer}
     */
    public Map<IRVirtualRegister, Integer> allocate(IRFunction fn) {
        map.clear();
        // 预注册全局变量对应槽号（全局槽区）
        for (GlobalVariable g : GlobalVariableTable.all()) {
            map.put(g.register(), g.slot());
        }

        int next = 0; // 下一个可分配的局部槽编号

        // 1. 为所有参数分配槽号，顺序与参数声明一致
        for (IRVirtualRegister param : fn.parameters()) {
            map.put(param, next++);
        }

        // 2. 线性遍历所有指令，分配未注册的目标寄存器和操作数槽号
        for (IRInstruction inst : fn.body()) {
            // 2.1 目标寄存器未分配，则分配新槽号
            if (inst.dest() != null && !map.containsKey(inst.dest())) {
                map.put(inst.dest(), nextFree(next));
                next = nextFree(next + 1);
            }
            // 2.2 操作数为虚拟寄存器且未分配，则分配新槽号
            for (IRValue operand : inst.operands()) {
                if (operand instanceof IRVirtualRegister vr && !map.containsKey(vr)) {
                    map.put(vr, nextFree(next));
                    next = nextFree(next + 1);
                }
            }
        }

        // 3. 返回只读映射，防止外部误操作
        return Map.copyOf(map);
    }

    /**
     * 获取下一个未被占用且不属于全局槽区的本地槽编号。
     * <p>
     * 用于寄存器分配过程中，保证槽号唯一且不与全局变量冲突。
     * </p>
     *
     * @param start 搜索起始槽号
     * @return 下一个可用的局部槽号（不在全局槽区且未被占用）
     */
    private int nextFree(int start) {
        int candidate = start;
        while (map.containsValue(candidate) || GlobalSlot.isGlobal(candidate)) {
            candidate++;
        }
        return candidate;
    }
}