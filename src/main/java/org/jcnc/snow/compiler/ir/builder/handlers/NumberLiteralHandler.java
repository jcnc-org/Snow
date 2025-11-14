package org.jcnc.snow.compiler.ir.builder.handlers;

import org.jcnc.snow.compiler.ir.builder.expression.ExpressionBuilder;
import org.jcnc.snow.compiler.ir.builder.expression.ExpressionHandler;
import org.jcnc.snow.compiler.ir.instruction.LoadConstInstruction;
import org.jcnc.snow.compiler.ir.utils.ExpressionUtils;
import org.jcnc.snow.compiler.ir.value.IRConstant;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.compiler.parser.ast.NumberLiteralNode;

/**
 * 数字字面量处理器。
 * <p>
 * 负责将整数、浮点数等数字字面量节点转为 IR 常量加载指令，自动处理数值类型。
 */
public class NumberLiteralHandler implements ExpressionHandler<NumberLiteralNode> {

    /**
     * 处理数字字面量表达式，生成常量加载指令。
     *
     * @param b 表达式构建器
     * @param n 数字字面量 AST 节点
     * @return 存放常量值的虚拟寄存器
     */
    @Override
    public IRVirtualRegister handle(ExpressionBuilder b, NumberLiteralNode n) {
        // 1. 自动判断数字类型，生成对应 IR 常量
        IRConstant c = ExpressionUtils.buildNumberConstant(b.ctx(), n.value());
        // 2. 分配新寄存器
        IRVirtualRegister r = b.ctx().newRegister();
        // 3. 加载常量指令
        b.ctx().addInstruction(new LoadConstInstruction(r, c));
        return r;
    }
}
