package org.jcnc.snow.compiler.parser.ast.base;

/**
 * {@code StatementNode} 表示抽象语法树（AST）中所有语句结构的统一接口。
 * <p>
 * 该接口为标记接口（Marker Interface），用于识别和区分语句类节点，
 * 包括但不限于变量声明、赋值语句、控制结构（如 if、loop）、返回语句等。
 * 实现此接口的类应表示程序在运行时执行的具体语法行为。
 * </p>
 */
public interface StatementNode extends Node {}
