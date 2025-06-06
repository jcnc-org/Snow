package org.jcnc.snow.compiler.parser.ast.base;

/**
 * {@code Node} 是抽象语法树（AST）中所有语法节点的统一根接口。
 * <p>
 * 作为标记接口（Marker Interface），该接口不定义任何方法，
 * 主要用于统一标识并组织 AST 体系中的各种语法构件节点，包括：
 * </p>
 * <ul>
 *     <li>{@link ExpressionNode}：表达式节点，如常量、变量引用、函数调用等</li>
 *     <li>{@link StatementNode}：语句节点，如声明、赋值、条件控制、循环、返回语句等</li>
 *     <li>模块、函数、参数等高层结构节点</li>
 * </ul>
 * <p>
 * 所有 AST 处理逻辑（如遍历、分析、代码生成）均可基于该接口实现统一调度和类型判定。
 * </p>
 */
public interface Node {}
