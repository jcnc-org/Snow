package org.jcnc.snow.compiler.ir.instruction;

import org.jcnc.snow.compiler.ir.core.IRInstruction;
import org.jcnc.snow.compiler.ir.core.IROpCode;
import org.jcnc.snow.compiler.ir.core.IRValue;
import org.jcnc.snow.compiler.ir.core.IRVisitor;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.List;

/**
 * BinaryOperationInstruction —— 表示一个二元运算指令，格式为: dest = lhs OP rhs
 * <p>
 * 该类用于描述形如 a = b + c 或 a = x * y 的二元运算指令。
 * 运算类型（OP）由 {@link IROpCode} 指定，包括加法、减法、乘法、除法等。
 * 左右操作数为 IRValue 类型，结果保存在目标虚拟寄存器 dest 中。
 */
public final class BinaryOperationInstruction extends IRInstruction {

    /**
     * 指令操作符，如 ADD_I32、SUB_I32 等，取自 IROpCode 枚举
     */
    private final IROpCode op;

    /**
     * 运算结果将写入的目标虚拟寄存器
     */
    private final IRVirtualRegister dest;

    /**
     * 运算的左操作数
     */
    private final IRValue lhs;

    /**
     * 运算的右操作数
     */
    private final IRValue rhs;

    /**
     * 构造函数，创建一个完整的二元运算指令。
     *
     * @param op   运算类型（加、减、乘、除等）
     * @param dest 运算结果的目标寄存器
     * @param lhs  左操作数
     * @param rhs  右操作数
     */
    public BinaryOperationInstruction(IROpCode op, IRVirtualRegister dest, IRValue lhs, IRValue rhs) {
        this.op = op;
        this.dest = dest;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    /**
     * 获取该指令的操作符。
     *
     * @return 运算类型（IROpCode）
     */
    @Override
    public IROpCode op() {
        return op;
    }

    /**
     * 获取该指令的目标寄存器。
     *
     * @return 运算结果将写入的虚拟寄存器
     */
    @Override
    public IRVirtualRegister dest() {
        return dest;
    }

    /**
     * 获取该指令使用的操作数。
     *
     * @return 一个包含左、右操作数的列表
     */
    @Override
    public List<IRValue> operands() {
        return List.of(lhs, rhs);
    }

    /**
     * 转换为字符串格式，便于调试与打印。
     * 例: v1 = ADD_I32 v2, v3
     *
     * @return 指令的字符串表示形式
     */
    @Override
    public String toString() {
        return dest + " = " + op + " " + lhs + ", " + rhs;
    }

    /**
     * 接受访问者对象，实现访问者模式分发逻辑。
     *
     * @param visitor 实现 IRVisitor 的访问者对象
     */
    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
