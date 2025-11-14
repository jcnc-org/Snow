package org.jcnc.snow.compiler.ir.builder.handlers;

import org.jcnc.snow.compiler.ir.builder.expression.ExpressionBuilder;
import org.jcnc.snow.compiler.ir.builder.expression.ExpressionHandler;
import org.jcnc.snow.compiler.ir.instruction.LoadConstInstruction;
import org.jcnc.snow.compiler.ir.value.IRConstant;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.compiler.parser.ast.StringLiteralNode;

/**
 * 字符串字面量处理器。
 * <p>
 * 负责将字符串常量（如 "abc"）节点转换为 IR 常量加载指令。
 */
public class StringLiteralHandler implements ExpressionHandler<StringLiteralNode> {

    /**
     * 处理字符串字面量表达式，生成常量加载指令。
     *
     * @param b 表达式构建器
     * @param n 字符串字面量 AST 节点
     * @return 存放字符串常量的虚拟寄存器
     */
    @Override
    public IRVirtualRegister handle(ExpressionBuilder b, StringLiteralNode n) {
        // 1. 创建字符串常量
        IRConstant c = new IRConstant(n.value());
        // 2. 分配新寄存器
        IRVirtualRegister r = b.ctx().newRegister();
        // 3. 加载常量指令到寄存器
        b.ctx().addInstruction(new LoadConstInstruction(r, c));
        return r;
    }
}
