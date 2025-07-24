package org.jcnc.snow.compiler.semantic.analyzers.statement;

import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.parser.ast.LoopNode;
import org.jcnc.snow.compiler.semantic.analyzers.TypeUtils;
import org.jcnc.snow.compiler.semantic.analyzers.base.StatementAnalyzer;
import org.jcnc.snow.compiler.semantic.core.Context;
import org.jcnc.snow.compiler.semantic.core.ModuleInfo;
import org.jcnc.snow.compiler.semantic.error.SemanticError;
import org.jcnc.snow.compiler.semantic.symbol.SymbolTable;
import org.jcnc.snow.compiler.semantic.type.Type;

/**
 * {@code LoopAnalyzer} 用于分析 for/while 等循环结构的语义正确性。
 * <p>
 * 主要职责如下: 
 * <ul>
 *   <li>为整个循环体（包括初始化、条件、更新、循环体本身）创建独立的块级符号表（作用域），保证循环内变量与外部隔离。</li>
 *   <li>依次分析初始化语句、条件表达式、更新语句和循环体各语句，并递归检查嵌套的语法结构。</li>
 *   <li>检查条件表达式的类型必须为 boolean，否则记录语义错误。</li>
 *   <li>支持所有错误的收集而不中断流程，便于一次性输出全部问题。</li>
 * </ul>
 * 该分析器实现了 SCompiler 语言的块级作用域循环与类型健壮性，是健全语义分析的基础部分。
 */
public class LoopAnalyzer implements StatementAnalyzer<LoopNode> {

    /**
     * 分析循环结构（如 for、while）的语义合法性。
     *
     * @param ctx    语义分析上下文（错误收集等）
     * @param mi     当前模块信息
     * @param fn     当前所在函数
     * @param locals 外部传入的符号表（本地作用域）
     * @param ln     当前循环节点
     */
    @Override
    public void analyze(Context ctx,
                        ModuleInfo mi,
                        FunctionNode fn,
                        SymbolTable locals,
                        LoopNode ln) {

        // 1. 创建整个循环结构的块级作用域
        // 新建 loopScope，以支持循环内部变量声明与外部隔离
        SymbolTable loopScope = new SymbolTable(locals);

        // 2. 分析初始化语句
        var initAnalyzer = ctx.getRegistry().getStatementAnalyzer(ln.init());
        if (initAnalyzer != null) {
            initAnalyzer.analyze(ctx, mi, fn, loopScope, ln.init());
        }

        // 3. 分析条件表达式
        var condAnalyzer = ctx.getRegistry().getExpressionAnalyzer(ln.cond());
        Type condType = condAnalyzer.analyze(ctx, mi, fn, loopScope, ln.cond());
        // 条件类型必须为 boolean，否则记录错误
        if (TypeUtils.isLogic(condType)) {
            ctx.getErrors().add(new SemanticError(ln, "loop 条件必须为 boolean"));
        }

        // 4. 分析更新语句
        var updateAnalyzer = ctx.getRegistry().getStatementAnalyzer(ln.step());
        if (updateAnalyzer != null) {
            updateAnalyzer.analyze(ctx, mi, fn, loopScope, ln.step());
        }

        // 5. 分析循环体内的每一条语句
        for (var stmt : ln.body()) {
            var stAnalyzer = ctx.getRegistry().getStatementAnalyzer(stmt);
            if (stAnalyzer != null) {
                // 递归分析循环体语句，作用域同样为 loopScope
                stAnalyzer.analyze(ctx, mi, fn, loopScope, stmt);
            } else {
                // 不支持的语句类型，记录错误
                ctx.getErrors().add(new SemanticError(stmt, "不支持的语句类型: " + stmt));
            }
        }
    }
}
