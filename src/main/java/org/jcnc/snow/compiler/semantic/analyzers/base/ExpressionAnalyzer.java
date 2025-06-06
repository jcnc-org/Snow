package org.jcnc.snow.compiler.semantic.analyzers.base;

import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.semantic.core.Context;
import org.jcnc.snow.compiler.semantic.core.ModuleInfo;
import org.jcnc.snow.compiler.semantic.symbol.SymbolTable;
import org.jcnc.snow.compiler.semantic.type.Type;

/**
 * 表达式分析器接口：定义了对 AST 中表达式节点进行语义分析的通用契约。
 * <p>
 * 各种具体的表达式分析器（如调用、二元运算、标识符、字面量等）需实现此接口，
 * 在 {@link #analyze(Context, ModuleInfo, FunctionNode, SymbolTable, ExpressionNode)}
 * 方法中完成类型推导、语义检查，并将发现的错误记录到上下文中。
 *
 * @param <E> 要分析的具体表达式节点类型，必须是 {@link ExpressionNode} 的子类型
 */
public interface ExpressionAnalyzer<E extends ExpressionNode> {

    /**
     * 对给定的表达式节点进行语义分析，并返回推导出的类型。
     * <p>
     * 实现者应在分析过程中根据节点语义：
     * <ul>
     *   <li>校验子表达式类型并递归调用对应的分析器；</li>
     *   <li>检查函数调用、运算符合法性；</li>
     *   <li>必要时向 {@link Context#getErrors()} 添加 {@link org.jcnc.snow.compiler.semantic.error.SemanticError}；</li>
     *   <li>返回最终推导出的 {@link Type}，以供上层表达式或语句分析使用。</li>
     * </ul>
     *
     * @param ctx    全局上下文，提供模块注册表、错误收集、日志输出及分析器注册表等
     * @param mi     当前模块信息，用于跨模块调用和函数签名查找
     * @param fn     当前函数节点，可用于返回类型校验或其他函数级上下文
     * @param locals 当前作用域的符号表，包含已声明的变量及其类型
     * @param expr   待分析的表达式节点
     * @return       表达式的推导类型，用于后续类型兼容性检查和类型传播
     */
    Type analyze(Context ctx,
                 ModuleInfo mi,
                 FunctionNode fn,
                 SymbolTable locals,
                 E expr);
}
