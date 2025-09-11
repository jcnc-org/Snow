package org.jcnc.snow.compiler.ir.builder.handlers;

import org.jcnc.snow.compiler.ir.builder.core.InstructionFactory;
import org.jcnc.snow.compiler.ir.builder.expression.ExpressionBuilder;
import org.jcnc.snow.compiler.ir.builder.expression.ExpressionHandler;
import org.jcnc.snow.compiler.ir.core.IROpCode;
import org.jcnc.snow.compiler.ir.instruction.UnaryOperationInstruction;
import org.jcnc.snow.compiler.ir.utils.ExpressionUtils;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.compiler.parser.ast.UnaryExpressionNode;

/**
 * 一元运算表达式处理器。
 * <p>
 * 负责将一元运算表达式（如 -x、!x）转换为对应的 IR 指令。
 * 支持负号取反、逻辑非等一元操作。
 */
public class UnaryHandler implements ExpressionHandler<UnaryExpressionNode> {

    /**
     * 处理一元表达式，生成对应 IR 指令。
     *
     * @param b  表达式构建器
     * @param un 一元表达式 AST 节点
     * @return 存放一元表达式结果的虚拟寄存器
     */
    @Override
    public IRVirtualRegister handle(ExpressionBuilder b, UnaryExpressionNode un) {
        // 1. 先构建操作数，得到其结果寄存器
        IRVirtualRegister src = b.build(un.operand());
        // 2. 分配目标寄存器
        IRVirtualRegister dest = b.ctx().newRegister();

        switch (un.operator()) {
            case "-" ->
                // 负号取反，调用 IR 一元取负指令
                    b.ctx().addInstruction(
                            new UnaryOperationInstruction(ExpressionUtils.negOp(un.operand()), dest, src));
            case "!" -> {
                // 逻辑非 !x：生成 x == 0 的比较指令
                IRVirtualRegister zero = InstructionFactory.loadConst(b.ctx(), 0);
                return InstructionFactory.binOp(b.ctx(), IROpCode.CMP_IEQ, src, zero);
            }
            default ->
                // 不支持的操作符抛出异常
                    throw new IllegalStateException("未知一元运算符: " + un.operator());
        }
        return dest;
    }
}
