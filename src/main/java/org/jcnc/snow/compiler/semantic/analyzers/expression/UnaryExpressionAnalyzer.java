package org.jcnc.snow.compiler.semantic.analyzers.expression;

import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.parser.ast.UnaryExpressionNode;
import org.jcnc.snow.compiler.semantic.analyzers.base.ExpressionAnalyzer;
import org.jcnc.snow.compiler.semantic.core.Context;
import org.jcnc.snow.compiler.semantic.core.ModuleInfo;
import org.jcnc.snow.compiler.semantic.error.SemanticError;
import org.jcnc.snow.compiler.semantic.symbol.SymbolTable;
import org.jcnc.snow.compiler.semantic.type.BuiltinType;
import org.jcnc.snow.compiler.semantic.type.Type;

/**
 * {@code UnaryExpressionAnalyzer} — 一元表达式的语义分析器。
 *
 * <p>目前实现两种一元运算:
 * <ul>
 *   <li>{@code -x} 取负: 仅允许作用于数值类型（int / float 等）。</li>
 *   <li>{@code !x} 逻辑非: 仅允许作用于 {@code boolean} 类型。</li>
 * </ul>
 *
 * <p>分析流程:
 * <ol>
 *   <li>递归分析操作数表达式，获取其类型 {@code operandType}。</li>
 *   <li>根据运算符检查类型合法性:
 *       <ul>
 *         <li>若类型不符，记录 {@link SemanticError} 并返回一个占位类型
 *             （取负返回 {@link BuiltinType#INT}，逻辑非返回
 *             {@link BuiltinType#BOOLEAN}）。</li>
 *         <li>若合法，则返回运算后的结果类型
 *             （取负为 {@code operandType}，逻辑非为 {@link BuiltinType#BOOLEAN}）。</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * <p>若遇到未支持的运算符，将生成错误并返回 {@code int} 作为占位类型。</p>
 *
 */
public class UnaryExpressionAnalyzer implements ExpressionAnalyzer<UnaryExpressionNode> {

    /**
     * 对一元表达式进行语义分析。
     *
     * @param ctx    全局编译上下文，持有错误列表、注册表等
     * @param mi     当前模块信息
     * @param fn     所在函数节点（可为 {@code null} 表示顶层）
     * @param locals 当前作用域符号表
     * @param expr   要分析的一元表达式节点
     * @return 表达式的结果类型；若有错误，返回占位类型并在 {@code ctx.getErrors()}
     * 中记录 {@link SemanticError}
     */
    @Override
    public Type analyze(Context ctx,
                        ModuleInfo mi,
                        FunctionNode fn,
                        SymbolTable locals,
                        UnaryExpressionNode expr) {

        /* ------------------------------------------------------------------
         * 1. 先递归分析操作数，确定其类型
         * ------------------------------------------------------------------ */
        Type operandType = ctx.getRegistry()
                .getExpressionAnalyzer(expr.operand())
                .analyze(ctx, mi, fn, locals, expr.operand());

        /* ------------------------------------------------------------------
         * 2. 根据运算符校验类型并给出结果类型
         * ------------------------------------------------------------------ */
        switch (expr.operator()) {
            /* -------------- 取负运算 -------------- */
            case "-" -> {
                if (!operandType.isNumeric()) {
                    ctx.getErrors().add(new SemanticError(
                            expr,
                            "'-' 只能应用于数值类型，当前为 " + operandType
                    ));
                    // 返回占位类型，避免后续阶段 NPE
                    return BuiltinType.INT;
                }
                // 合法: 结果类型与操作数相同
                return operandType;
            }

            /* -------------- 逻辑非运算 -------------- */
            case "!" -> {
                if (operandType != BuiltinType.BOOLEAN) {
                    ctx.getErrors().add(new SemanticError(
                            expr,
                            "'!' 只能应用于 boolean 类型，当前为 " + operandType
                    ));
                    return BuiltinType.BOOLEAN;
                }
                // 合法: 结果类型恒为 boolean
                return BuiltinType.BOOLEAN;
            }

            /* -------------- 未知运算符 -------------- */
            default -> {
                ctx.getErrors().add(new SemanticError(
                        expr,
                        "未知一元运算符: " + expr.operator()
                ));
                return BuiltinType.INT;
            }
        }
    }
}
