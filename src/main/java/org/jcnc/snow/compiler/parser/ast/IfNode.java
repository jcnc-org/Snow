package org.jcnc.snow.compiler.parser.ast;

import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;

import java.util.List;

/**
 * {@code IfNode} 表示抽象语法树（AST）中的条件语句结构（if-else）。
 * <p>
 * 该节点包含一个条件表达式（condition）、一个 then 分支语句列表，
 * 以及一个可选的 else 分支语句列表。
 * </p>
 * <p>
 * 条件表达式为布尔类型，决定是否执行 then 分支。
 * 若 condition 为假，则执行 else 分支（如果提供）。
 * </p>
 * <p>
 * 示例语法结构：
 * </p>
 * <pre>{@code
 * if (x > 0) {
 *     print("Positive");
 * } else {
 *     print("Negative");
 * }
 * }</pre>
 *
 * @param condition  控制分支执行的条件表达式
 * @param thenBranch 条件为 true 时执行的语句块
 * @param elseBranch 条件为 false 时执行的语句块（可为空）
 */
public record IfNode(
        ExpressionNode condition,
        List<StatementNode> thenBranch,
        List<StatementNode> elseBranch
) implements StatementNode {
}