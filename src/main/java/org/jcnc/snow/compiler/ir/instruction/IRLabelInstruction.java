package org.jcnc.snow.compiler.ir.instruction;

import org.jcnc.snow.compiler.ir.core.IRInstruction;
import org.jcnc.snow.compiler.ir.core.IROpCode;
import org.jcnc.snow.compiler.ir.core.IRVisitor;

/**
 * 在 IR 中标记一个代码位置，用于 Jump / 条件跳转目标。
 * <p>
 * 生成到 VM 时并不会真正发出可执行指令，
 * 仅在 {@code VMCodeGenerator} 内部被用来计算真实地址。
 */
public final class IRLabelInstruction extends IRInstruction {
    private final String name;

    public IRLabelInstruction(String name) {
        this.name = name;
    }

    @Override
    public IROpCode op() {
        return IROpCode.LABEL;
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name + ":";
    }

    /** 目前尚未对 Label 做访问者处理，空实现即可 */
    @Override
    public void accept(IRVisitor visitor) {
        /* no-op */
    }
}
