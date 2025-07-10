package org.jcnc.snow.compiler.parser.ast;

import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;

/**
 * 表示布尔字面量（boolean literal）的抽象语法树（AST）节点。
 * <p>
 * 该纪录类实现 {@link ExpressionNode} 接口，用于在编译器前端构建语法分析过程中，
 * 表达布尔类型的字面量常量（如 "true" 或 "false"）。
 * </p>
 *
 * @param value    字面量的布尔值
 * @param line     当前节点所在的行号
 * @param column   当前节点所在的列号
 * @param file     当前节点所在的文件
 */
public record BoolLiteralNode(
        boolean value,
        int line,
        int column,
        String file
) implements ExpressionNode {

    /**
     * 使用布尔字面量字符串构造一个 {@code BoolLiteralNode} 实例。
     * <p>
     * 本构造方法接受一个字符串词素（lexeme），并通过 {@link Boolean#parseBoolean(String)} 解析为布尔值。
     * 如果传入的字符串为 "true"（忽略大小写），则解析结果为 {@code true}；否则为 {@code false}。
     * </p>
     *
     * @param lexeme 布尔字面量的字符串表示
     */
    public BoolLiteralNode(String lexeme, int line, int column, String file) {
        this(Boolean.parseBoolean(lexeme), line, column, file);
    }

    /**
     * 返回此布尔字面量节点的值。
     *
     * @return 布尔值，代表源代码中的布尔字面量
     */
    public boolean getValue() {
        return value;
    }
}
