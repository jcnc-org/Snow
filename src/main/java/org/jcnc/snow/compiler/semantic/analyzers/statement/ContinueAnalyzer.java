package org.jcnc.snow.compiler.semantic.analyzers.statement;

import org.jcnc.snow.compiler.parser.ast.ContinueNode;
import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.semantic.analyzers.base.StatementAnalyzer;
import org.jcnc.snow.compiler.semantic.core.Context;
import org.jcnc.snow.compiler.semantic.core.ModuleInfo;
import org.jcnc.snow.compiler.semantic.symbol.SymbolTable;

/**
 * {@code ContinueAnalyzer} —— continue 语句的语义分析器。
 * <p>
 * 当前实现不做任何额外检查。continue 是否出现在有效的循环体内，
 * 由 IR 构建阶段（如 StatementBuilder）负责判定与错误提示。
 * </p>
 * <p>
 * 用于 AST 的 {@link ContinueNode} 语句节点。
 * </p>
 */
public class ContinueAnalyzer implements StatementAnalyzer<ContinueNode> {

    /**
     * 分析 continue 语句节点的语义。
     * <p>
     * 该方法为 no-op，相关语义约束由后续阶段处理。
     * </p>
     *
     * @param ctx    语义分析上下文
     * @param mi     当前模块信息
     * @param fn     当前所在函数节点
     * @param locals 当前作用域下的符号表
     * @param stmt   需要分析的 continue 语句节点
     */
    @Override
    public void analyze(Context ctx,
                        ModuleInfo mi,
                        FunctionNode fn,
                        SymbolTable locals,
                        ContinueNode stmt) {
        // no-op: continue 语句的合法性由 IR 构建阶段检查
    }
}
