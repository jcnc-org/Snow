package org.jcnc.snow.compiler.parser.ast;

import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;

/**
 * 一元表达式节点，例如 -x 或 !x。
 *
 * @param operator 运算符字符串 ("-" / "!")
 * @param operand  操作数表达式
 */
public record UnaryExpressionNode(String operator,
                                  ExpressionNode operand) implements ExpressionNode {

    @Override public String toString() {
        return operator + operand;
    }
}
