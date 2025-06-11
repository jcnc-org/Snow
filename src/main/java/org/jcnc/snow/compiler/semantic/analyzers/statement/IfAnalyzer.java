package org.jcnc.snow.compiler.semantic.analyzers.statement;

import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.parser.ast.IfNode;
import org.jcnc.snow.compiler.semantic.analyzers.TypeUtils;
import org.jcnc.snow.compiler.semantic.analyzers.base.StatementAnalyzer;
import org.jcnc.snow.compiler.semantic.core.Context;
import org.jcnc.snow.compiler.semantic.core.ModuleInfo;
import org.jcnc.snow.compiler.semantic.error.SemanticError;
import org.jcnc.snow.compiler.semantic.symbol.SymbolTable;
import org.jcnc.snow.compiler.semantic.type.Type;

/**
 * {@code IfAnalyzer} 用于分析 if 语句的语义正确性。
 * <p>
 * 主要职责如下：
 * <ul>
 *   <li>条件表达式类型检查：确认 if 的条件表达式类型为 boolean，否则记录语义错误。</li>
 *   <li>块级作用域：分别为 then 分支和 else 分支创建独立的符号表（SymbolTable），
 *       支持分支内变量的块级作用域，防止分支内声明的变量污染外部或互相干扰，允许分支内变量同名遮蔽。</li>
 *   <li>分支递归分析：对 then 和 else 分支的每条语句递归调用对应的语义分析器，进行语义检查。</li>
 *   <li>错误记录：若遇到条件类型不符、不支持的语句类型或分支内部其他语义问题，均通过 {@link SemanticError} 记录详细错误信息，并附带代码位置信息。</li>
 *   <li>健壮性：不会因一处错误立即终止，而是尽量分析全部分支，收集所有能发现的错误，一次性输出。</li>
 * </ul>
 * <p>
 * 该分析器提升了语言的健壮性与可维护性，是支持 SCompiler 块级作用域及全局错误收集能力的关键一环。
 */
public class IfAnalyzer implements StatementAnalyzer<IfNode> {

    /**
     * 分析 if 语句的语义合法性，包括条件表达式类型、分支作用域及分支语句检查。
     *
     * @param ctx    语义分析上下文（记录全局符号表和错误）
     * @param mi     当前模块信息
     * @param fn     当前所在函数
     * @param locals 当前作用域符号表
     * @param ifn    if 语句 AST 节点
     */
    @Override
    public void analyze(Context ctx,
                        ModuleInfo mi,
                        FunctionNode fn,
                        SymbolTable locals,
                        IfNode ifn) {

        // 1. 检查 if 条件表达式类型
        // 获取对应条件表达式的表达式分析器
        var exprAnalyzer = ctx.getRegistry().getExpressionAnalyzer(ifn.condition());
        // 对条件表达式执行类型分析
        Type condType = exprAnalyzer.analyze(ctx, mi, fn, locals, ifn.condition());
        // 判断条件类型是否为 boolean，否则报错
        if (TypeUtils.isLogic(condType)) {
            ctx.getErrors().add(new SemanticError(ifn, "if 条件必须为 boolean"));
        }

        // 2. 分析 then 分支
        // 创建 then 分支的块级作用域（以当前 locals 为父作用域）
        SymbolTable thenScope = new SymbolTable(locals);
        // 遍历 then 分支下的每一条语句
        for (var stmt : ifn.thenBranch()) {
            // 获取对应语句类型的分析器
            var stAnalyzer = ctx.getRegistry().getStatementAnalyzer(stmt);
            if (stAnalyzer != null) {
                // 对当前语句执行语义分析（作用域为 thenScope）
                stAnalyzer.analyze(ctx, mi, fn, thenScope, stmt);
            } else {
                // 若找不到对应的分析器，记录错误
                ctx.getErrors().add(new SemanticError(stmt, "不支持的语句类型: " + stmt));
            }
        }

        // 3. 分析 else 分支（可选）
        if (!ifn.elseBranch().isEmpty()) {
            // 创建 else 分支的块级作用域（同样以 locals 为父作用域）
            SymbolTable elseScope = new SymbolTable(locals);
            // 遍历 else 分支下的每一条语句
            for (var stmt : ifn.elseBranch()) {
                var stAnalyzer = ctx.getRegistry().getStatementAnalyzer(stmt);
                if (stAnalyzer != null) {
                    // 对当前语句执行语义分析（作用域为 elseScope）
                    stAnalyzer.analyze(ctx, mi, fn, elseScope, stmt);
                } else {
                    ctx.getErrors().add(new SemanticError(stmt, "不支持的语句类型: " + stmt));
                }
            }
        }
    }
}
