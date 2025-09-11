package org.jcnc.snow.compiler.parser.ast;

import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;

/**
 * {@code NumberLiteralNode} 表示抽象语法树（AST）中的数字字面量表达式节点。
 * <p>
 * 用于表示源代码中的数值常量，如整数 {@code 42} 或浮点数 {@code 3.14}。
 * 为了兼容不同数值格式，本节点以字符串形式存储原始值，
 * 在语义分析或类型推导阶段再行解析为具体数值类型。
 * </p>
 *
 * @param value   数字字面量的原始字符串表示
 * @param context 节点上下文信息（包含行号、列号等）
 */
public record NumberLiteralNode(
        String value,
        NodeContext context
) implements ExpressionNode {

    /**
     * 返回数字字面量的字符串形式。
     *
     * @return 字面量原始字符串值（例如 "42" 或 "3.14"）
     */
    @Override
    public String toString() {
        return value;
    }
}
