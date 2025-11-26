package org.jcnc.snow.compiler.semantic.analyzers.statement;

import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.parser.ast.ReturnNode;
import org.jcnc.snow.compiler.semantic.analyzers.base.StatementAnalyzer;
import org.jcnc.snow.compiler.semantic.core.Context;
import org.jcnc.snow.compiler.semantic.core.ModuleInfo;
import org.jcnc.snow.compiler.semantic.error.SemanticError;
import org.jcnc.snow.compiler.semantic.symbol.SymbolTable;
import org.jcnc.snow.compiler.semantic.type.BuiltinType;
import org.jcnc.snow.compiler.semantic.type.FunctionType;
import org.jcnc.snow.compiler.semantic.type.Type;
import org.jcnc.snow.compiler.semantic.utils.NumericConstantUtils;

/**
 * {@code ReturnAnalyzer} 是用于分析 {@link ReturnNode} 返回语句的语义分析器。
 * <p>
 * 它负责检查函数中的 return 语句是否与函数定义的返回类型匹配。分析流程包括:
 * <ul>
 *   <li>获取当前函数的返回类型 {@link FunctionType#returnType()}；</li>
 *   <li>若 return 语句包含返回值表达式，检查其类型与函数定义是否兼容；</li>
 *   <li>若 return 语句未指定返回值，而函数返回类型非 {@link BuiltinType#VOID}，则视为错误；</li>
 *   <li>所有不兼容情况将记录为 {@link SemanticError} 并写入分析日志。</li>
 * </ul>
 */
public class ReturnAnalyzer implements StatementAnalyzer<ReturnNode> {

    /**
     * 分析 return 语句的语义合法性。
     *
     * @param ctx    当前语义分析上下文对象，提供模块访问、错误记录与分析器调度功能。
     * @param mi     当前模块信息，用于定位当前函数定义。
     * @param fn     当前所在的函数节点，包含函数名及参数定义。
     * @param locals 当前作用域的符号表（return 不依赖变量声明，此参数未使用）。
     * @param ret    {@link ReturnNode} 语法节点，表示函数中的 return 语句，可能包含返回值表达式。
     */
    @Override
    public void analyze(Context ctx,
                        ModuleInfo mi,
                        FunctionNode fn,
                        SymbolTable locals,
                        ReturnNode ret) {

        ctx.log("检查 return");

        // 获取当前函数的定义信息
        FunctionType expected = ctx.getModules()
                .get(mi.getName())
                .getFunctions()
                .get(fn.name());

        // 情况 1: 存在返回表达式，需进行类型检查
        ret.getExpression().ifPresentOrElse(exp -> {
            var exprAnalyzer = ctx.getRegistry().getExpressionAnalyzer(exp);
            Type actual = exprAnalyzer.analyze(ctx, mi, fn, locals, exp);

            boolean ok = expected.returnType().isCompatible(actual)
                    || NumericConstantUtils.canNarrowToIntegral(expected.returnType(), actual, exp);

            if (!ok) {
                ctx.getErrors().add(new SemanticError(
                        ret,
                        "return 类型不匹配: 期望 "
                                + expected.returnType()
                                + ", 实际 "
                                + actual
                ));
                ctx.log("错误: return 类型不匹配");
            }

            // 情况 2: 无返回表达式，但函数定义了非 void 返回类型
        }, () -> {
            if (expected.returnType() != BuiltinType.VOID) {
                ctx.getErrors().add(new SemanticError(
                        ret,
                        "非 void 函数必须返回值"
                ));
                ctx.log("错误: 非 void 函数缺少返回值");
            }
        });
    }
}
