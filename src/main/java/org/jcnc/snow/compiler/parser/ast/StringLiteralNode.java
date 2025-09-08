package org.jcnc.snow.compiler.parser.ast;

import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;

/**
 * {@code StringLiteralNode} 表示抽象语法树（AST）中的字符串字面量表达式节点。
 * <p>
 * 用于表示源代码中出现的字符串常量，如 {@code "hello"}、{@code "abc123"} 等。
 * 节点内部仅保存不带引号的字符串内容，便于后续语义处理或编码。
 * </p>
 *
 * @param value   字符串常量的内容，原始值中不包含双引号
 * @param context 节点上下文信息（包含行号、列号等）
 */
public record StringLiteralNode(
        String value,
        NodeContext context
) implements ExpressionNode {

    /**
     * 返回字符串字面量的带引号表示，适用于语法树调试或文本输出。
     * <p>
     * 例如，当 {@code value = Result:} 时，返回 {@code "Result:"}。
     * </p>
     *
     * @return 字符串字面量的完整表示形式（带双引号）
     */
    @Override
    public String toString() {
        return "\"" + value + "\"";
    }
}
