package org.jcnc.snow.compiler.ir.value;

import org.jcnc.snow.compiler.ir.core.IRValue;

/**
 * IRVirtualRegister —— 表示一个静态单赋值（SSA）形式的虚拟寄存器。
 * <p>
 * 在 IR 系统中，虚拟寄存器用于存储每个中间计算结果，是 SSA（Static Single Assignment）形式的核心。
 * 每个虚拟寄存器在程序中只被赋值一次，其值来源于一条明确的指令输出。
 * <p>
 * 特点：
 * <ul>
 *   <li>每个寄存器有唯一编号 {@code id}，由 {@code IRFunction.newRegister()} 自动生成</li>
 *   <li>实现 {@link IRValue} 接口，可作为 IRInstruction 的操作数</li>
 *   <li>具备良好的打印与调试格式：%id</li>
 * </ul>
 *
 * 适用于表达式求值、参数传递、函数返回值、临时变量等所有中间值场景。
 *
 * @param id 寄存器的唯一编号，通常从 0 开始递增
 */
public record IRVirtualRegister(int id) implements IRValue {

    /**
     * 将虚拟寄存器转换为字符串格式，方便输出和调试。
     * 格式为：%<id>，例如 %3 表示编号为 3 的虚拟寄存器。
     *
     * @return 格式化的字符串表示
     */
    @Override
    public String toString() {
        return "%" + id;
    }
}
