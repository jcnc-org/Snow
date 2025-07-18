package org.jcnc.snow.compiler.parser.ast;

import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;

/**
 * {@code BinaryExpressionNode} 表示抽象语法树（AST）中的二元运算表达式节点。
 * <p>
 * 二元表达式通常由两个操作数和一个中间操作符构成，例如 {@code a + b}。
 * 此结构广泛用于数学计算、逻辑判断、字符串拼接等语法结构中。
 * </p>
 *
 * @param left     左操作数（子表达式）
 * @param operator 运算符字符串（如 "+", "-", "*", "/" 等）
 * @param right    右操作数（子表达式）
 * @param context  节点上下文（包含行号、列号等信息）
 */
public record BinaryExpressionNode(
        ExpressionNode left,
        String operator,
        ExpressionNode right,
        NodeContext context
) implements ExpressionNode {

    /**
     * 返回该二元运算表达式的字符串表示形式。
     * <p>
     * 输出格式为: {@code left + " " + operator + " " + right}，
     * 适用于调试或打印语法树结构。
     * </p>
     *
     * @return 表示该二元表达式的字符串
     */
    @Override
    public String toString() {
        return left + " " + operator + " " + right;
    }
}
