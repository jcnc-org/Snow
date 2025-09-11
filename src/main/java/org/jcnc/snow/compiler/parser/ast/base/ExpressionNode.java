package org.jcnc.snow.compiler.parser.ast.base;

/**
 * {@code ExpressionNode} 表示抽象语法树（AST）中所有表达式类型节点的统一接口。
 * <p>
 * 作为标记接口（Marker Interface），该接口用于区分表达式与其他语法结构，
 * 不定义具体方法，其子类型通常表示可参与求值运算的结构，
 * 如常量表达式、变量引用、函数调用、算术运算等。
 * </p>
 * <p>
 * 所有实现此接口的节点可参与表达式求值、语义分析、类型检查与中间代码生成等处理流程。
 * </p>
 */
public interface ExpressionNode extends Node {
}