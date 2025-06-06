package org.jcnc.snow.compiler.ir.instruction;

import org.jcnc.snow.compiler.ir.core.IRInstruction;
import org.jcnc.snow.compiler.ir.core.IROpCode;
import org.jcnc.snow.compiler.ir.core.IRVisitor;

/**
 * IRJumpInstruction —— 表示一个无条件跳转（jump）的 IR 指令。
 * <p>
 * 该指令用于控制流结构中，实现无条件跳转到指定标签（label）。
 * 是 if-else、循环、函数跳转等高级语言结构翻译到中间表示的重要组成部分。
 */
public class IRJumpInstruction extends IRInstruction {

    /** 跳转目标的标签名 */
    private final String label;

    /**
     * 构造函数，创建跳转指令。
     *
     * @param label 跳转目标标签的名称
     */
    public IRJumpInstruction(String label) {
        this.label = label;
    }

    /**
     * 获取该指令对应的操作码：JUMP。
     *
     * @return IROpCode.JUMP
     */
    @Override
    public IROpCode op() {
        return IROpCode.JUMP;
    }

    /**
     * 获取跳转目标标签名。
     *
     * @return 标签名称字符串
     */
    public String label() {
        return label;
    }

    /**
     * 接受访问者，用于访问者模式处理。
     *
     * @param visitor 实现 IRVisitor 的访问者实例
     */
    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }

    /**
     * 将指令转为字符串形式，便于打印与调试。
     * 例如：jump L1
     *
     * @return 指令的字符串表示
     */
    @Override
    public String toString() {
        return "jump " + label;
    }
}
