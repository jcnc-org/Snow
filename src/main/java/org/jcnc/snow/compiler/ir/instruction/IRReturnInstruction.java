package org.jcnc.snow.compiler.ir.instruction;

import org.jcnc.snow.compiler.ir.core.IRInstruction;
import org.jcnc.snow.compiler.ir.core.IROpCode;
import org.jcnc.snow.compiler.ir.core.IRValue;
import org.jcnc.snow.compiler.ir.core.IRVisitor;

import java.util.List;

/**
 * IRReturnInstruction —— 表示一个带返回值的返回（ret）指令。
 * <p>
 * 此指令用于函数结束时将某个值作为返回结果返回给调用者。
 * 返回值可以是常量、寄存器或表达式的结果。
 * 若不返回值，也可以扩展为 null 代表 void 函数。
 */
public class IRReturnInstruction extends IRInstruction {

    /**
     * 要返回的值，可以是常量、虚拟寄存器等
     */
    private final IRValue returnValue;

    /**
     * 构造函数，创建返回指令。
     *
     * @param returnValue 函数的返回值
     */
    public IRReturnInstruction(IRValue returnValue) {
        this.returnValue = returnValue;
    }

    /**
     * 获取该指令的操作码: RET。
     *
     * @return IROpCode.RET，表示返回操作
     */
    @Override
    public IROpCode op() {
        return IROpCode.RET;
    }

    /**
     * 返回该指令的操作数列表（仅包含返回值）。
     *
     * @return 含一个元素的列表，即返回值
     */
    @Override
    public List<IRValue> operands() {
        return List.of(returnValue);
    }

    /**
     * 接受访问者处理该指令，适用于访问者模式。
     *
     * @param visitor 实现了 IRVisitor 的访问者对象
     */
    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }

    /**
     * 转换为字符串形式，便于调试与打印。
     * 示例: ret v1
     *
     * @return 字符串形式的返回指令
     */
    @Override
    public String toString() {
        return "ret " + returnValue;
    }
}
