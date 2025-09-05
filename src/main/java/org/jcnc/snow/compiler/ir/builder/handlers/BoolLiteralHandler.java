package org.jcnc.snow.compiler.ir.builder.handlers;

import org.jcnc.snow.compiler.ir.builder.expression.ExpressionBuilder;
import org.jcnc.snow.compiler.ir.builder.expression.ExpressionHandler;
import org.jcnc.snow.compiler.ir.instruction.LoadConstInstruction;
import org.jcnc.snow.compiler.ir.value.IRConstant;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.compiler.parser.ast.BoolLiteralNode;

/**
 * 布尔字面量处理器。
 * <p>
 * 负责将布尔类型字面量（true/false）转换为 IR 常量指令，统一采用 1 表示 true，0 表示 false。
 */
public class BoolLiteralHandler implements ExpressionHandler<BoolLiteralNode> {

    /**
     * 构建布尔字面量，生成加载常量的 IR 指令，并返回结果寄存器。
     *
     * @param b 表达式构建器
     * @param n 布尔字面量 AST 节点
     * @return 存放 true/false 的虚拟寄存器（1 或 0）
     */
    @Override
    public IRVirtualRegister handle(ExpressionBuilder b, BoolLiteralNode n) {
        // 1. true 映射为 1，false 映射为 0，统一为整型常量
        IRConstant c = new IRConstant(n.getValue() ? 1 : 0);
        // 2. 分配新寄存器
        IRVirtualRegister r = b.ctx().newRegister();
        // 3. 生成加载常量指令
        b.ctx().addInstruction(new LoadConstInstruction(r, c));
        return r;
    }
}
