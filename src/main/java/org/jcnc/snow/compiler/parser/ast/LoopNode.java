package org.jcnc.snow.compiler.parser.ast;

import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;

import java.util.List;

/**
 * {@code LoopNode} 表示抽象语法树（AST）中的循环语句结构。
 * <p>
 * 该节点建模了类似传统 {@code for} 循环的控制结构，
 * 包含初始化语句、循环条件、更新语句及循环体。
 * 每一部分均对应为 AST 中的子节点，便于进一步语义分析与代码生成。
 * </p>
 *
 * @param init 在循环开始前执行的初始化语句
 * @param cond   每次迭代前评估的条件表达式，控制循环是否继续
 * @param step      每轮迭代完成后执行的更新语句
 * @param body        循环体语句列表，表示循环主体执行逻辑
 * @param context     节点的上下文信息（包含行号、列号、文件名等）
 */
public record LoopNode(
        StatementNode init,
        ExpressionNode cond,
        StatementNode step,
        List<StatementNode> body,
        NodeContext context
) implements StatementNode {
}
