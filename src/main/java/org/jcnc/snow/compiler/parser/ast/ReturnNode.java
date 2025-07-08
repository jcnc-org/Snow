package org.jcnc.snow.compiler.parser.ast;

import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;

import java.util.Optional;

/**
 * {@code ReturnNode} 表示抽象语法树（AST）中的 return 语句节点。
 * <p>
 * return 语句用于从当前函数中返回控制权，并可携带一个可选的返回值表达式。
 * </p>
 * <p>
 * 示例：
 * <ul>
 *   <li>{@code return;}</li>
 *   <li>{@code return x + 1;}</li>
 * </ul>
 * </p>
 */
public class ReturnNode implements StatementNode {

    /** 可选的返回值表达式 */
    private final Optional<ExpressionNode> expression;

    /** 节点上下文信息（包含行号、列号等） */
    private final NodeContext context;

    /**
     * 构造一个 {@code ReturnNode} 实例。
     *
     * @param expression 返回值表达式，如果无返回值则可为 {@code null}
     * @param context    节点上下文信息（包含行号、列号等）
     */
    public ReturnNode(ExpressionNode expression, NodeContext context) {
        this.expression = Optional.ofNullable(expression);
        this.context = context;
    }

    /**
     * 获取可选的返回值表达式。
     *
     * @return 如果有返回值则返回 {@code Optional.of(expression)}，否则返回 {@code Optional.empty()}
     */
    public Optional<ExpressionNode> getExpression() {
        return expression;
    }

    /**
     * 获取节点上下文信息（包含行号、列号等）。
     *
     * @return NodeContext 实例
     */
    @Override
    public NodeContext context() {
        return context;
    }
}
