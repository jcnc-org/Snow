package org.jcnc.snow.compiler.parser.ast;

import org.jcnc.snow.compiler.parser.ast.base.Node;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;

import java.util.List;

/**
 * {@code FunctionNode} 表示抽象语法树（AST）中的函数定义结构。
 * <p>
 * 函数定义通常包含函数名、形参列表、返回类型以及函数体，
 * 在语义分析、类型检查与代码生成等阶段具有核心地位。
 * 示例：{@code int add(int a, int b) { return a + b; }}
 * </p>
 *
 * @param name       函数名称标识符
 * @param parameters 参数列表，每项为 {@link ParameterNode} 表示一个形参定义
 * @param returnType 函数的返回类型（如 "int"、"void" 等）
 * @param body       函数体语句块，由一组 {@link StatementNode} 构成
 * @param line     当前节点所在的行号
 * @param column   当前节点所在的列号
 * @param file     当前节点所在的文件
 */
public record FunctionNode(
        String name,
        List<ParameterNode> parameters,
        String returnType,
        List<StatementNode> body,
        int line,
        int column,
        String file
) implements Node {
}