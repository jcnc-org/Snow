package org.jcnc.snow.compiler.parser.ast;

import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;

/**
 * {@code MemberExpressionNode} 表示抽象语法树（AST）中的成员访问表达式节点。
 * <p>
 * 用于表示对象字段或方法的访问操作，语法形式如 {@code object.member}。
 * 成员访问常见于结构体、模块、对象导入等上下文中，是表达式链中常见的构件之一。
 * </p>
 *
 * @param object   左侧对象表达式，表示成员所属的作用域或容器
 * @param member   要访问的成员名称（字段名或方法名）
 * @param context  节点上下文信息（包含行号、列号等）
 */
public record MemberExpressionNode(
        ExpressionNode object,
        String member,
        NodeContext context
) implements ExpressionNode {

    /**
     * 返回成员访问表达式的字符串形式。
     * <p>
     * 输出格式为 {@code object.member}，用于调试或语法树可视化。
     * </p>
     *
     * @return 成员访问表达式的字符串形式
     */
    @Override
    public String toString() {
        return object + "." + member;
    }
}
