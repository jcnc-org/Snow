package org.jcnc.snow.compiler.ir.builder.handlers;

import org.jcnc.snow.compiler.ir.builder.core.InstructionFactory;
import org.jcnc.snow.compiler.ir.builder.expression.ExpressionBuilder;
import org.jcnc.snow.compiler.ir.builder.expression.ExpressionHandler;
import org.jcnc.snow.compiler.ir.core.IROpCode;
import org.jcnc.snow.compiler.ir.utils.ComparisonUtils;
import org.jcnc.snow.compiler.ir.utils.ExpressionUtils;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.compiler.parser.ast.BinaryExpressionNode;
import org.jcnc.snow.compiler.parser.ast.StringLiteralNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;

/**
 * {@code BinaryHandler} 实现二元运算表达式的 IR 指令生成逻辑。
 *
 * <p>
 * 负责将二元运算表达式（如 {@code +}, {@code -}, {@code *}, {@code /}, {@code ==}, {@code <} 等）AST 节点
 * 统一转换为底层 IR 指令序列，区分比较运算、字符串拼接和普通算术/位运算等。
 * </p>
 *
 * <p>
 * <b>主要行为说明：</b>
 * <ul>
 *   <li>识别字符串拼接：如有一侧为字符串常量或类型为 {@code string}，{@code +} 运算符视为拼接</li>
 *   <li>识别并转换所有常见二元比较运算符（==, !=, <, >, <=, >= 等）</li>
 *   <li>其余支持的算术与位运算符自动查找对应的 IR 操作码</li>
 *   <li>所有转换结果均以虚拟寄存器表达，可选支持将结果写入指定寄存器，方便优化</li>
 *   <li>不支持的运算符将抛出 {@link IllegalStateException}</li>
 * </ul>
 * </p>
 */
public class BinaryHandler implements ExpressionHandler<BinaryExpressionNode> {

    /**
     * 构建二元运算表达式，将结果存入新分配的虚拟寄存器并返回。
     *
     * <p>
     * 根据不同类型的运算符，分派不同 IR 生成策略：
     * <ul>
     *   <li>字符串拼接 {@code +} —— 用 ADD_R 指令，结果寄存器标记为 string 类型</li>
     *   <li>比较运算符 —— 依赖 ComparisonUtils 查找对应的 cmp IR 指令</li>
     *   <li>普通二元运算 —— 通过 ExpressionUtils 查找合适的 IR 操作码</li>
     * </ul>
     * </p>
     *
     * @param b   表达式构建器，提供上下文和递归能力
     * @param bin 二元表达式 AST 节点
     * @return 结果虚拟寄存器
     * @throws IllegalStateException 若遇到不支持的运算符
     */
    @Override
    public IRVirtualRegister handle(ExpressionBuilder b, BinaryExpressionNode bin) {
        // 1. 分别构建左右子表达式
        IRVirtualRegister a = b.build(bin.left());
        IRVirtualRegister c = b.build(bin.right());
        String op = bin.operator();

        // 2. 字符串拼接识别
        boolean isStringConcat = "+".equals(op) &&
                (isStringLike(b, bin.left(), a) || isStringLike(b, bin.right(), c));

        if (isStringConcat) {
            IRVirtualRegister out = InstructionFactory.binOp(b.ctx(), IROpCode.ADD_R, a, c);
            b.ctx().getScope().setRegisterType(out, "string");
            return out;
        }

        // 3. 比较运算符
        if (ComparisonUtils.isComparisonOperator(op)) {
            return InstructionFactory.binOp(
                    b.ctx(),
                    ComparisonUtils.cmpOp(
                            b.ctx().getScope().getVarTypes(),
                            op, bin.left(), bin.right()),
                    a, c);
        }

        // 4. 普通二元运算
        IROpCode code = ExpressionUtils.resolveOpCode(op, bin.left(), bin.right());
        if (code == null) {
            throw new IllegalStateException("不支持的运算符: " + op);
        }
        return InstructionFactory.binOp(b.ctx(), code, a, c);
    }

    /**
     * 构建二元运算表达式，并将结果直接写入指定目标虚拟寄存器。
     *
     * <p>
     * 可用于减少中间寄存器分配（如优化代码生成），与 {@link #handle} 逻辑一致，只是结果寄存器由调用方指定。
     * </p>
     *
     * @param b    表达式构建器
     * @param bin  二元表达式 AST 节点
     * @param dest 目标结果寄存器
     * @throws IllegalStateException 若遇到不支持的运算符
     */
    @Override
    public void handleInto(ExpressionBuilder b, BinaryExpressionNode bin, IRVirtualRegister dest) {
        IRVirtualRegister a = b.build(bin.left());
        IRVirtualRegister c = b.build(bin.right());
        String op = bin.operator();

        boolean isStringConcat = "+".equals(op) &&
                (isStringLike(b, bin.left(), a) || isStringLike(b, bin.right(), c));

        if (isStringConcat) {
            InstructionFactory.binOpInto(b.ctx(), IROpCode.ADD_R, a, c, dest);
            b.ctx().getScope().setRegisterType(dest, "string");
            return;
        }

        if (ComparisonUtils.isComparisonOperator(op)) {
            InstructionFactory.binOpInto(
                    b.ctx(),
                    ComparisonUtils.cmpOp(
                            b.ctx().getScope().getVarTypes(),
                            op, bin.left(), bin.right()),
                    a, c, dest);
        } else {
            IROpCode code = ExpressionUtils.resolveOpCode(op, bin.left(), bin.right());
            if (code == null) {
                throw new IllegalStateException("不支持的运算符: " + op);
            }
            InstructionFactory.binOpInto(b.ctx(), code, a, c, dest);
        }
    }

    /**
     * 判断表达式节点或其求值结果是否为字符串相关。
     *
     * @param b        表达式构建器
     * @param expr     待判断的表达式 AST
     * @param valueReg 表达式求值后的虚拟寄存器
     * @return 若表达式或结果寄存器为字符串类型/字面量，返回 true
     */
    private boolean isStringLike(ExpressionBuilder b, ExpressionNode expr, IRVirtualRegister valueReg) {
        if (expr instanceof StringLiteralNode) {
            return true;
        }
        String type = b.ctx().getScope().getRegisterType(valueReg);
        if (type != null && type.equalsIgnoreCase("string")) {
            return true;
        }
        if (expr instanceof BinaryExpressionNode bin) {
            return containsStringLiteral(bin);
        }
        return false;
    }

    /**
     * 递归判断表达式树中是否含有字符串字面量。
     *
     * @param expr 表达式节点
     * @return 若子树中存在字符串字面量，返回 true
     */
    private boolean containsStringLiteral(ExpressionNode expr) {
        if (expr instanceof StringLiteralNode) {
            return true;
        }
        if (expr instanceof BinaryExpressionNode bin) {
            return containsStringLiteral(bin.left()) || containsStringLiteral(bin.right());
        }
        return false;
    }
}
