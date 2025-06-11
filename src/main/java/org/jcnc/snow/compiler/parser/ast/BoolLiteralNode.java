package org.jcnc.snow.compiler.parser.ast;

import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;

/**
 * 表示布尔字面量（boolean literal）的抽象语法树（AST）节点。
 * <p>
 * 本类实现了 {@link ExpressionNode} 接口，用于在编译器前端构建语法分析过程中，
 * 表达布尔类型的字面量常量（如 "true" 或 "false"）。
 * </p>
 */
public class BoolLiteralNode implements ExpressionNode {
    /**
     * 字面量的布尔值。
     */
    private final boolean value;

    /**
     * 使用布尔字面量字符串构造一个 {@code BoolLiteralNode} 实例。
     * <p>
     * 本构造方法接受一个字符串词素（lexeme），并通过 {@link Boolean#parseBoolean(String)} 解析为布尔值。
     * 如果传入的字符串为 "true"（忽略大小写），则解析结果为 {@code true}；否则为 {@code false}。
     * </p>
     *
     * @param lexeme 布尔字面量的字符串表示
     */
    public BoolLiteralNode(String lexeme) {
        this.value = Boolean.parseBoolean(lexeme);
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
