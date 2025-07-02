package org.jcnc.snow.compiler.parser.ast;

import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;

/**
 * {@code NumberLiteralNode} 表示抽象语法树（AST）中的数字字面量表达式节点。
 * <p>
 * 用于表示源代码中的数值常量，如整数 {@code 42} 或浮点数 {@code 3.14}。
 * 为了兼容不同数值格式，本节点以字符串形式存储原始值，
 * 在语义分析或类型推导阶段再行解析为具体数值类型。
 * </p>
 *
 * @param value    数字字面量的原始字符串表示
 * @param line     当前节点所在的行号
 * @param column   当前节点所在的列号
 * @param file     当前节点所在的文件
 */
public record NumberLiteralNode(
        String value,
        int line,
        int column,
        String file
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
