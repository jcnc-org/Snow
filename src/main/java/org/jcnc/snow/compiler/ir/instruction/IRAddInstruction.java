package org.jcnc.snow.compiler.ir.instruction;

import org.jcnc.snow.compiler.ir.core.IRInstruction;
import org.jcnc.snow.compiler.ir.core.IROpCode;
import org.jcnc.snow.compiler.ir.core.IRValue;
import org.jcnc.snow.compiler.ir.core.IRVisitor;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.List;

/**
 * IRAddInstruction —— 表示一个加法指令，形如：dest = lhs + rhs
 * <p>
 * 本类是一个具体的 IRInstruction 子类，表示将两个值相加，并将结果写入目标寄存器的操作。
 * 虽然功能与通用的 {@link BinaryOperationInstruction} 类似，但它作为更简化明确的指令实现，
 * 通常用于测试或示例用途，也可为特殊优化保留独立形式。
 */
public class IRAddInstruction extends IRInstruction {

    /** 运算结果存放的目标虚拟寄存器 */
    private final IRVirtualRegister dest;

    /** 左操作数 */
    private final IRValue lhs;

    /** 右操作数 */
    private final IRValue rhs;

    /**
     * 构造函数，创建加法指令实例。
     *
     * @param dest 运算结果的存储位置
     * @param lhs  加法左操作数
     * @param rhs  加法右操作数
     */
    public IRAddInstruction(IRVirtualRegister dest, IRValue lhs, IRValue rhs) {
        this.dest = dest;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    /**
     * 返回该指令的操作码：ADD_I32。
     *
     * @return 加法操作码
     */
    @Override
    public IROpCode op() {
        return IROpCode.ADD_I32;
    }

    /**
     * 获取指令的目标寄存器。
     *
     * @return 运算结果存放的虚拟寄存器
     */
    @Override
    public IRVirtualRegister dest() {
        return dest;
    }

    /**
     * 获取加法指令的两个操作数。
     *
     * @return 包含左、右操作数的列表
     */
    @Override
    public List<IRValue> operands() {
        return List.of(lhs, rhs);
    }

    /**
     * 使用访问者处理当前加法指令。
     *
     * @param visitor 实现 IRVisitor 的访问者对象
     */
    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }

    /**
     * 返回指令的字符串形式，方便调试。
     * 例如：v1 = v2 + v3
     *
     * @return 字符串表示形式
     */
    @Override
    public String toString() {
        return dest + " = " + lhs + " + " + rhs;
    }
}
