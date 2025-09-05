package org.jcnc.snow.compiler.ir.builder.handlers;

import org.jcnc.snow.compiler.ir.builder.core.InstructionFactory;
import org.jcnc.snow.compiler.ir.builder.expression.ExpressionBuilder;
import org.jcnc.snow.compiler.ir.builder.expression.ExpressionHandler;
import org.jcnc.snow.compiler.ir.core.IROpCode;
import org.jcnc.snow.compiler.ir.utils.ComparisonUtils;
import org.jcnc.snow.compiler.ir.utils.ExpressionUtils;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.compiler.parser.ast.BinaryExpressionNode;

/**
 * 二元表达式处理器。
 * <p>
 * 负责将二元运算表达式（如 +, -, *, /, ==, <, ...）节点转换为对应的 IR 指令。
 * 区分比较运算符与普通算术/位运算等，分别分派不同的 IR 生成策略。
 */
public class BinaryHandler implements ExpressionHandler<BinaryExpressionNode> {

    /**
     * 构建二元表达式，返回结果寄存器。
     *
     * @param b   表达式构建器
     * @param bin 二元表达式 AST 节点
     * @return 存放结果的虚拟寄存器
     * @throws IllegalStateException 不支持的运算符会抛出异常
     */
    @Override
    public IRVirtualRegister handle(ExpressionBuilder b, BinaryExpressionNode bin) {
        // 1. 分别构建左右子表达式，得到对应的结果寄存器
        IRVirtualRegister a = b.build(bin.left());
        IRVirtualRegister c = b.build(bin.right());
        String op = bin.operator();

        // 2. 判断是否为比较运算符（==, !=, <, >, <=, >= 等）
        if (ComparisonUtils.isComparisonOperator(op)) {
            return InstructionFactory.binOp(
                    b.ctx(),
                    // 自动分析类型，选择正确的比较指令
                    ComparisonUtils.cmpOp(
                            b.ctx().getScope().getVarTypes(),
                            op, bin.left(), bin.right()),
                    a, c);
        }

        // 3. 普通二元运算（+ - * / & | ^ ...），查找对应 IR 操作码
        IROpCode code = ExpressionUtils.resolveOpCode(op, bin.left(), bin.right());
        if (code == null) {
            throw new IllegalStateException("不支持的运算符: " + op);
        }
        return InstructionFactory.binOp(b.ctx(), code, a, c);
    }

    /**
     * 将二元表达式的结果直接写入指定目标寄存器（用于减少中间寄存器分配，便于优化）。
     *
     * @param b    表达式构建器
     * @param bin  二元表达式 AST 节点
     * @param dest 目标虚拟寄存器
     * @throws IllegalStateException 不支持的运算符会抛出异常
     */
    @Override
    public void handleInto(ExpressionBuilder b, BinaryExpressionNode bin, IRVirtualRegister dest) {
        // 1. 分别构建左右子表达式
        IRVirtualRegister a = b.build(bin.left());
        IRVirtualRegister c = b.build(bin.right());
        String op = bin.operator();

        // 2. 比较运算符直接写入目标寄存器
        if (ComparisonUtils.isComparisonOperator(op)) {
            InstructionFactory.binOpInto(
                    b.ctx(),
                    ComparisonUtils.cmpOp(
                            b.ctx().getScope().getVarTypes(),
                            op, bin.left(), bin.right()),
                    a, c, dest);
        } else {
            // 3. 普通二元运算符
            IROpCode code = ExpressionUtils.resolveOpCode(op, bin.left(), bin.right());
            if (code == null) {
                throw new IllegalStateException("不支持的运算符: " + op);
            }
            InstructionFactory.binOpInto(b.ctx(), code, a, c, dest);
        }
    }
}
