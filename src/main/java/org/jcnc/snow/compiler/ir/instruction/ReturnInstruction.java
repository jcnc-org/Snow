package org.jcnc.snow.compiler.ir.instruction;

import org.jcnc.snow.compiler.ir.core.IRInstruction;
import org.jcnc.snow.compiler.ir.core.IROpCode;
import org.jcnc.snow.compiler.ir.core.IRValue;
import org.jcnc.snow.compiler.ir.core.IRVisitor;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.List;

/**
 * ReturnInstruction —— 表示函数返回指令，格式：RET 或 RET <value>
 * <p>
 * 此类用于描述函数执行完毕后的返回操作。支持两种返回形式：
 * - 无返回值（void）：生成无参的 RET 指令
 * - 有返回值：将指定虚拟寄存器中的值返回给调用者
 * <p>
 * 与 {@link IRReturnInstruction} 类似，但更通用，适配多种函数返回风格。
 */
public final class ReturnInstruction extends IRInstruction {

    /**
     * 返回值所在的虚拟寄存器。
     * 如果为 null，则代表函数无返回值（即 void）。
     */
    private final IRVirtualRegister value;

    /**
     * 构造函数，创建返回指令实例。
     *
     * @param value 若函数有返回值，传入对应虚拟寄存器；
     *              若为 void 函数，则传 null。
     */
    public ReturnInstruction(IRVirtualRegister value) {
        this.value = value;
    }

    /**
     * 返回该指令的操作码类型：RET。
     *
     * @return IROpCode.RET
     */
    @Override
    public IROpCode op() {
        return IROpCode.RET;
    }

    /**
     * 获取该指令的操作数。
     * 如果为 void 返回，则返回空列表；
     * 否则返回一个仅包含返回寄存器的列表。
     *
     * @return 操作数列表
     */
    @Override
    public List<IRValue> operands() {
        return value == null ? List.of() : List.of(value);
    }

    /**
     * 获取返回值所在的虚拟寄存器（如有）。
     *
     * @return 返回值寄存器，或 null（表示 void）
     */
    public IRVirtualRegister value() {
        return value;
    }

    /**
     * 转换为字符串形式，便于调试与输出。
     * - 无返回值：RET
     * - 有返回值：RET v1
     *
     * @return 字符串表示的返回指令
     */
    @Override
    public String toString() {
        return value == null ? "RET" : "RET " + value;
    }

    /**
     * 接受访问者对象，实现访问者模式分发。
     *
     * @param visitor 实现 IRVisitor 的访问者
     */
    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }
}
