package org.jcnc.snow.compiler.ir.instruction;

import org.jcnc.snow.compiler.ir.core.IRInstruction;
import org.jcnc.snow.compiler.ir.core.IROpCode;
import org.jcnc.snow.compiler.ir.core.IRValue;
import org.jcnc.snow.compiler.ir.core.IRVisitor;
import org.jcnc.snow.compiler.ir.value.IRConstant;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.List;

/**
 * LoadConstInstruction —— 表示一个常量加载指令，格式为：dest = CONST k
 * <p>
 * 该指令的功能是将一个常量（字面量或编译期已知值）加载到一个虚拟寄存器中，
 * 供后续指令使用。例如，在表达式计算、参数传递、初始化等场景中常用。
 */
public final class LoadConstInstruction extends IRInstruction {

    /** 要加载的常量值，类型为 IRConstant */
    private final IRConstant k;

    /** 存放常量结果的目标虚拟寄存器 */
    private final IRVirtualRegister dest;

    /**
     * 构造函数，创建常量加载指令。
     *
     * @param dest 存放常量的目标虚拟寄存器
     * @param k    要加载的常量值
     */
    public LoadConstInstruction(IRVirtualRegister dest, IRConstant k) {
        this.dest = dest;
        this.k = k;
    }

    /**
     * 获取该指令的操作码，固定为 CONST。
     *
     * @return IROpCode.CONST
     */
    @Override
    public IROpCode op() {
        return IROpCode.CONST;
    }

    /**
     * 获取指令的目标虚拟寄存器。
     *
     * @return 用于存放常量的寄存器
     */
    @Override
    public IRVirtualRegister dest() {
        return dest;
    }

    /**
     * 获取该指令的操作数（仅包含要加载的常量）。
     *
     * @return 含一个元素（k）的操作数列表
     */
    @Override
    public List<IRValue> operands() {
        return List.of(k);
    }

    /**
     * 返回该指令的字符串形式，便于调试或打印。
     * 例如：v1 = CONST 42
     *
     * @return 指令的字符串表示
     */
    @Override
    public String toString() {
        return dest + " = CONST " + k;
    }

    /**
     * 接受访问者模式处理。
     *
     * @param visitor 实现 IRVisitor 的访问者对象
     */
    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
