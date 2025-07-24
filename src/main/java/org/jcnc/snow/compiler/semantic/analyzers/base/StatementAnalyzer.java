package org.jcnc.snow.compiler.semantic.analyzers.base;

import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;
import org.jcnc.snow.compiler.semantic.core.Context;
import org.jcnc.snow.compiler.semantic.core.ModuleInfo;
import org.jcnc.snow.compiler.semantic.symbol.SymbolTable;

/**
 * 语句分析器接口: 定义如何对 AST 中的语句节点执行语义检查。
 * <p>
 * 各具体的语句分析器（如声明、赋值、分支、循环、返回等）需实现此接口，
 * 在 {@link #analyze(Context, ModuleInfo, FunctionNode, SymbolTable, StatementNode)}
 * 方法中完成:
 * <ul>
 *   <li>对自身语义结构进行校验（变量声明、类型匹配、作用域检查等）；</li>
 *   <li>递归调用其他已注册的语句或表达式分析器；</li>
 *   <li>在发现错误时，通过 {@link Context#getErrors()} 记录 {@link org.jcnc.snow.compiler.semantic.error.SemanticError}；</li>
 *   <li>在上下文需要时记录日志以辅助调试。</li>
 * </ul>
 *
 * @param <S> 要分析的具体语句节点类型，必须是 {@link StatementNode} 的子类型
 */
public interface StatementAnalyzer<S extends StatementNode> {

    /**
     * 对给定的语句节点执行语义分析。
     *
     * @param ctx    全局上下文，提供模块注册表、错误收集、日志输出及分析器注册表等
     * @param mi     当前模块信息，用于检查模块导入和函数签名等上下文
     * @param fn     当前函数节点，可用于检查返回类型或函数级别作用域
     * @param locals 当前作用域的符号表，包含已声明的变量及其类型
     * @param stmt   待分析的语句节点实例
     */
    void analyze(Context ctx,
                 ModuleInfo mi,
                 FunctionNode fn,
                 SymbolTable locals,
                 S stmt);
}
