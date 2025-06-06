package org.jcnc.snow.compiler.parser.ast;

import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;

import java.util.Optional;

/**
 * {@code DeclarationNode} 表示抽象语法树（AST）中的变量声明语句节点。
 * <p>
 * 变量声明用于在语法层引入新的标识符及其类型信息，
 * 通常格式为 {@code type name = initializer;}，其中初始化表达式可省略。
 * </p>
 */
public class DeclarationNode implements StatementNode {

    /** 声明的变量名称 */
    private final String name;

    /** 变量的数据类型（如 "int", "string"） */
    private final String type;

    /** 可选的初始化表达式 */
    private final Optional<ExpressionNode> initializer;

    /**
     * 构造一个 {@code DeclarationNode} 实例。
     *
     * @param name        变量名称
     * @param type        变量类型字符串（如 "int"、"string"）
     * @param initializer 可选初始化表达式，若为 {@code null} 表示未初始化
     */
    public DeclarationNode(String name, String type, ExpressionNode initializer) {
        this.name = name;
        this.type = type;
        this.initializer = Optional.ofNullable(initializer);
    }

    /**
     * 获取变量名称。
     *
     * @return 变量名字符串
     */
    public String getName() {
        return name;
    }

    /**
     * 获取变量类型字符串。
     *
     * @return 类型名称（如 "int"）
     */
    public String getType() {
        return type;
    }

    /**
     * 获取可选的初始化表达式。
     *
     * @return 一个 Optional 包装的初始化表达式对象，可能为空
     */
    public Optional<ExpressionNode> getInitializer() {
        return initializer;
    }
}