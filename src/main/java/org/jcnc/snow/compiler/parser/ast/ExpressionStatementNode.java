package org.jcnc.snow.compiler.parser.ast;

import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;

/**
 * {@code ExpressionStatementNode} 表示抽象语法树（AST）中的表达式语句节点。
 * <p>
 * 表达式语句通常由一个单独的表达式组成，并以语句形式出现。
 * 例如：{@code foo();}、{@code x = 1;}、{@code print("hello");} 等。
 * </p>
 *
 * @param expression 表达式主体，通常为函数调用、赋值、方法链式调用等可求值表达式。
 * @param context    节点上下文信息（包含行号、列号等）
 */
public record ExpressionStatementNode(
        ExpressionNode expression,
        NodeContext context
) implements StatementNode {
}
