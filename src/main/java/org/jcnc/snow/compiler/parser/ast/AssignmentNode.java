package org.jcnc.snow.compiler.parser.ast;

import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;

/**
 * {@code AssignmentNode} 表示抽象语法树（AST）中的赋值语句节点。
 * <p>
 * 赋值语句用于将右侧表达式的值存储到左侧指定的变量中，
 * 通常形式为 {@code x = expression}，其中 {@code x} 是目标变量，
 * {@code expression} 是用于计算赋值结果的表达式。
 * </p>
 * <p>
 * 该节点作为语句节点的一种实现，适用于语义分析、类型检查、IR 构建等多个阶段。
 * </p>
 *
 * @param variable 左值变量名（即赋值目标）
 * @param value    表达式右值（即赋值来源）
 * @param line     当前节点所在的行号
 * @param column   当前节点所在的列号
 * @param file     当前节点所在的文件
 */
public record AssignmentNode(
        String variable,
        ExpressionNode value,
        int line,
        int column,
        String file
) implements StatementNode {

    /**
     * 返回赋值语句的字符串形式，便于调试与日志输出。
     * <p>
     * 典型格式形如 {@code x = y + 1}。
     * </p>
     *
     * @return 表示赋值语句的字符串形式
     */
    @Override
    public String toString() {
        return variable + " = " + value;
    }
}