package org.jcnc.snow.compiler.semantic.analyzers.expression;

import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.parser.ast.IdentifierNode;
import org.jcnc.snow.compiler.semantic.analyzers.base.ExpressionAnalyzer;
import org.jcnc.snow.compiler.semantic.core.Context;
import org.jcnc.snow.compiler.semantic.core.ModuleInfo;
import org.jcnc.snow.compiler.semantic.error.SemanticError;
import org.jcnc.snow.compiler.semantic.symbol.Symbol;
import org.jcnc.snow.compiler.semantic.symbol.SymbolTable;
import org.jcnc.snow.compiler.semantic.type.BuiltinType;
import org.jcnc.snow.compiler.semantic.type.Type;

/**
 * {@code IdentifierAnalyzer} 是用于分析标识符表达式（如变量名、常量名）的语义分析器。
 * <p>
 * 它的主要职责是在给定的局部作用域中查找标识符对应的符号定义，并返回其类型信息。
 * 如果标识符未在当前作用域内声明，则: 
 * <ul>
 *   <li>向语义错误列表中添加一条 {@link SemanticError} 记录；</li>
 *   <li>为保证分析过程的连续性，默认返回 {@link BuiltinType#INT} 类型作为降级处理。</li>
 * </ul>
 * <p>
 * 该分析器通常用于处理表达式中的变量引用或常量引用场景。
 */
public class IdentifierAnalyzer implements ExpressionAnalyzer<IdentifierNode> {

    /**
     * 对标识符表达式进行语义分析，判断其是否在当前作用域中已声明，并返回其类型。
     *
     * @param ctx    当前语义分析上下文对象，提供模块信息、错误收集、日志记录等服务。
     * @param mi     当前模块信息（此实现中未使用，但保留用于扩展）。
     * @param fn     当前分析的函数节点（此实现中未使用，但保留用于扩展）。
     * @param locals 当前函数或代码块的符号表，记录已声明的变量及其类型信息。
     * @param id     表达式中出现的 {@link IdentifierNode} 实例，表示待解析的标识符名称。
     * @return 若标识符已在符号表中声明，则返回对应的 {@link Type}；
     *         否则返回 {@link BuiltinType#INT} 作为错误降级类型。
     */
    @Override
    public Type analyze(Context ctx,
                        ModuleInfo mi,
                        FunctionNode fn,
                        SymbolTable locals,
                        IdentifierNode id) {

        // 在当前作用域中查找符号（变量或常量）
        Symbol sym = locals.resolve(id.name());
        if (sym == null) {
            // 未声明标识符: 记录语义错误，返回降级类型
            ctx.getErrors().add(new SemanticError(id,
                    "未声明的标识符: " + id.name()));
            ctx.log("错误: 未声明的标识符 " + id.name());
            return BuiltinType.INT;
        }

        // 返回符号的类型信息
        return sym.type();
    }
}
