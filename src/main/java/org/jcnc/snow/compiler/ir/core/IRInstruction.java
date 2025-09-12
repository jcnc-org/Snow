package org.jcnc.snow.compiler.ir.core;

import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.List;

/**
 * IRInstruction —— 所有 IR（中间表示）指令的抽象基类。
 * <p>
 * 本类定义了编译器中间表示系统中所有指令的基本结构和行为。
 * 具体指令通过继承此类并实现各自的操作码（Opcode）和访问者方法，
 * 以支持统一的指令处理和访问模式。
 * </p>
 */
public abstract class IRInstruction {

    /**
     * 获取该指令的操作码（Opcode）。
     * <p>
     * 每个具体指令子类必须实现此方法，返回对应的 IROpCode 枚举值。
     * </p>
     *
     * @return 表示指令类型的 IROpCode 实例
     */
    public abstract IROpCode op();

    /**
     * 获取指令的目标虚拟寄存器（destination register）。
     * <p>
     * 默认实现返回 null；只有具有目标寄存器的指令（如赋值、算术运算）
     * 应重写此方法以返回相应的 IRVirtualRegister。
     * </p>
     *
     * @return 目标虚拟寄存器，若无目标寄存器则返回 null
     */
    public IRVirtualRegister dest() {
        return null;
    }

    /**
     * 获取指令的操作数列表。
     * <p>
     * 默认实现返回空列表；具体指令子类应根据需要重写此方法，
     * 提供所有参与运算或调用的 IRValue 操作数集合。
     * </p>
     *
     * @return 包含本指令所有操作数的列表
     */
    public List<IRValue> operands() {
        return List.of();
    }

    /**
     * 接受一个 IRVisitor 实例，实现访问者模式的入口。
     * <p>
     * 具体指令子类必须实现此方法，以便 IRVisitor 根据指令类型
     * 调用相应的访问逻辑。
     * </p>
     *
     * @param visitor 实现 IRVisitor 接口的访问者对象
     */
    public abstract void accept(IRVisitor visitor);
}