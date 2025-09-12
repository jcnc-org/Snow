package org.jcnc.snow.compiler.parser.ast;

import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;

/**
 * {@code UnaryExpressionNode} —— 前缀一元运算 AST 节点。
 *
 * <p>代表两种受支持的一元前缀表达式:
 * <ul>
 *   <li><b>取负</b>: {@code -x}</li>
 *   <li><b>逻辑非</b>: {@code !x}</li>
 * </ul>
 * <p>
 * {@link #equals(Object)}、{@link #hashCode()} 等方法。</p>
 *
 * @param operator 一元运算符（仅 "-" 或 "!"）
 * @param operand  运算对象 / 右操作数
 * @param context  节点上下文信息（包含行号、列号等）
 */
public record UnaryExpressionNode(
        String operator,
        ExpressionNode operand,
        NodeContext context
) implements ExpressionNode {

    /**
     * 生成调试友好的字符串表示，例如 {@code "-x"} 或 {@code "!flag"}。
     *
     * @return 一元表达式的串表示
     */
    @Override
    public String toString() {
        return operator + operand;
    }
}
