package org.jcnc.snow.compiler.ir.instruction;

import org.jcnc.snow.compiler.ir.core.IRInstruction;
import org.jcnc.snow.compiler.ir.core.IROpCode;
import org.jcnc.snow.compiler.ir.core.IRValue;
import org.jcnc.snow.compiler.ir.core.IRVisitor;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.List;

/**
 * UnaryOperationInstruction —— 表示一个一元运算指令，格式: dest = OP val
 * <p>
 * 用于对单个操作数 val 执行指定的一元运算 OP（例如取负 NEG），
 * 并将结果写入目标虚拟寄存器 dest。
 * <p>
 * 支持的操作由 {@link IROpCode} 定义，目前常见的一元操作包括:
 * <ul>
 *   <li>NEG_I32 —— 整数取负: dest = -val</li>
 *   <li>（可扩展）逻辑非、按位非等</li>
 * </ul>
 */
public final class UnaryOperationInstruction extends IRInstruction {

    /**
     * 一元运算操作符（如 NEG_I32）
     */
    private final IROpCode op;

    /**
     * 运算结果写入的目标虚拟寄存器
     */
    private final IRVirtualRegister dest;

    /**
     * 被操作的值（唯一操作数）
     */
    private final IRValue val;

    /**
     * 构造函数，创建一元运算指令。
     *
     * @param op   一元运算操作符
     * @param dest 运算结果目标寄存器
     * @param val  参与运算的操作数
     */
    public UnaryOperationInstruction(IROpCode op, IRVirtualRegister dest, IRValue val) {
        this.op = op;
        this.dest = dest;
        this.val = val;
    }

    /**
     * 获取该指令的操作码。
     *
     * @return 一元运算的操作码（如 NEG_I32）
     */
    @Override
    public IROpCode op() {
        return op;
    }

    /**
     * 获取该指令的目标寄存器。
     *
     * @return 运算结果的目标寄存器
     */
    @Override
    public IRVirtualRegister dest() {
        return dest;
    }

    /**
     * 获取指令的操作数（仅一个）。
     *
     * @return 单元素列表，仅包含 val
     */
    @Override
    public List<IRValue> operands() {
        return List.of(val);
    }

    /**
     * 将该指令格式化为字符串，便于打印与调试。
     * 形式: dest = OP val，例如: v1 = NEG v2
     *
     * @return 字符串形式的指令
     */
    @Override
    public String toString() {
        return dest + " = " + op + " " + val;
    }

    /**
     * 接受访问者访问该指令，实现访问者模式。
     *
     * @param visitor 访问者实例
     */
    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
