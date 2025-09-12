package org.jcnc.snow.compiler.ir.builder.statement.utils;

import org.jcnc.snow.compiler.ir.builder.core.InstructionFactory;
import org.jcnc.snow.compiler.ir.builder.statement.StatementBuilderContext;
import org.jcnc.snow.compiler.ir.core.IROpCode;
import org.jcnc.snow.compiler.ir.utils.ComparisonUtils;
import org.jcnc.snow.compiler.ir.utils.IROpCodeUtils;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.compiler.parser.ast.BinaryExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;

/**
 * 条件跳转指令工具类。
 * <p>
 * 负责根据条件表达式（可为比较运算或一般表达式），
 * 自动生成“条件为假时跳转”到指定目标标签的中间代码指令。
 * <ul>
 *     <li>优先识别常见比较表达式（==、&gt;、&lt;等），直接转为IR的条件跳转。</li>
 *     <li>其他情况将表达式与0进行比较（等价于C风格的“是否为真”语义）。</li>
 * </ul>
 * 该类为静态工具类，不可实例化。
 * </p>
 */
public final class ConditionalJump {
    /**
     * 禁止实例化
     */
    private ConditionalJump() {
    }

    /**
     * 发射条件跳转指令：如果 cond 不成立，则跳转到 falseLabel。
     *
     * @param cond       条件表达式节点（可为二元比较或任意表达式）
     * @param falseLabel 条件为假时跳转的目标标签名
     * @param c          语句构建上下文（提供IR环境与表达式构建能力）
     */
    public static void emit(ExpressionNode cond, String falseLabel, StatementBuilderContext c) {
        // 情况1：如果是二元比较表达式（如a < b、x == y）
        if (cond instanceof BinaryExpressionNode(
                ExpressionNode left,
                String operator,
                ExpressionNode right,
                NodeContext _
        ) && ComparisonUtils.isComparisonOperator(operator)) {

            // 左右表达式分别生成虚拟寄存器
            IRVirtualRegister a = c.expr().build(left);
            IRVirtualRegister b = c.expr().build(right);

            // 获取IR层比较操作码，并取其“相反条件”作为跳转触发条件
            IROpCode cmp = ComparisonUtils.cmpOp(c.ctx().getScope().getVarTypes(), operator, left, right);
            IROpCode falseOp = IROpCodeUtils.invert(cmp);
            InstructionFactory.cmpJump(c.ctx(), falseOp, a, b, falseLabel);
            return;
        }

        // 情况2：其余情况（如标识符、调用、单目、复合表达式等）
        // 按“与0比较等于0”进行条件跳转
        IRVirtualRegister condReg = c.expr().build(cond);
        IRVirtualRegister zero = InstructionFactory.loadConst(c.ctx(), 0);
        InstructionFactory.cmpJump(c.ctx(), IROpCode.CMP_IEQ, condReg, zero, falseLabel);
    }
}
