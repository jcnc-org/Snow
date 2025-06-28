package org.jcnc.snow.compiler.parser.ast;

import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;

/**
 * {@code UnaryExpressionNode} —— 前缀一元运算 AST 节点。
 *
 * <p>代表两种受支持的一元前缀表达式：
 * <ul>
 *   <li><b>取负</b>：{@code -x}</li>
 *   <li><b>逻辑非</b>：{@code !x}</li>
 * </ul>
 *
 * {@link #equals(Object)}、{@link #hashCode()} 等方法。</p>
 *
 * @param operator 一元运算符（仅 "-" 或 "!"）
 * @param operand  运算对象 / 右操作数
 * @param line     当前节点所在的行号
 * @param column   当前节点所在的列号
 * @param file     当前节点所在的文件
 */
public record UnaryExpressionNode(
        String operator,
        ExpressionNode operand,
        int line,
        int column,
        String file
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
