package org.jcnc.snow.compiler.parser.ast;

import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;

/**
 * {@code ExpressionStatementNode} 表示抽象语法树（AST）中的表达式语句节点。
 * <p>
 * 表达式语句通常由一个单独的表达式组成，并以语句形式出现。
 * 例如：{@code foo();}、{@code x = 1;}、{@code print("hello");} 等。
 * </p>
 *
 * @param expression 表达式主体，通常为函数调用、赋值、方法链式调用等可求值表达式。
 */
public record ExpressionStatementNode(ExpressionNode expression) implements StatementNode {
}