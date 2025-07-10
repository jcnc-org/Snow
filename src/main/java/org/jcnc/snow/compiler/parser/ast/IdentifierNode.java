package org.jcnc.snow.compiler.parser.ast;

import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;

/**
 * {@code IdentifierNode} 表示抽象语法树（AST）中的标识符表达式节点。
 * <p>
 * 该节点用于表示变量名、函数名、字段名等符号引用。
 * 在语义分析中，通常需要将此类节点绑定到其声明位置或符号表项。
 * </p>
 *
 * @param name    标识符的文本名称（如变量名 "x"，函数名 "foo"）
 * @param context 节点上下文信息（包含行号、列号等）
 */
public record IdentifierNode(
        String name,
        NodeContext context
) implements ExpressionNode {

    /**
     * 返回标识符节点的字符串形式，通常为其名称本身。
     *
     * @return 标识符名称字符串
     */
    @Override
    public String toString() {
        return name;
    }
}
