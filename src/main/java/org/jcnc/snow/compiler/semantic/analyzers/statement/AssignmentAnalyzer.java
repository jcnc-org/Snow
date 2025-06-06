package org.jcnc.snow.compiler.semantic.analyzers.statement;

import org.jcnc.snow.compiler.parser.ast.AssignmentNode;
import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.semantic.analyzers.base.StatementAnalyzer;
import org.jcnc.snow.compiler.semantic.core.Context;
import org.jcnc.snow.compiler.semantic.core.ModuleInfo;
import org.jcnc.snow.compiler.semantic.error.SemanticError;
import org.jcnc.snow.compiler.semantic.symbol.*;
import org.jcnc.snow.compiler.semantic.type.Type;

/**
 * {@code AssignmentAnalyzer} 是赋值语句的语义分析器。
 * <p>
 * 负责分析和验证赋值语句的合法性，包括：
 * <ul>
 *   <li>变量是否已声明且可赋值（必须为 {@link SymbolKind#VARIABLE} 类型）；</li>
 *   <li>赋值右值的类型是否与变量类型兼容；</li>
 *   <li>是否允许进行数值类型的自动宽化转换（如 {@code int → float}）。</li>
 * </ul>
 * 若类型不兼容且无法自动宽化，则将记录语义错误并输出日志信息。
 */
public class AssignmentAnalyzer implements StatementAnalyzer<AssignmentNode> {

    /**
     * 分析赋值语句的语义有效性，包括左值合法性与类型匹配性。
     *
     * @param ctx    当前语义分析上下文，包含模块表、错误收集、日志输出等服务。
     * @param mi     当前模块信息，用于支持跨模块语义校验（当前未涉及）。
     * @param fn     当前分析的函数节点，提供局部作用域上下文。
     * @param locals 当前函数或代码块的符号表，用于变量解析与类型信息获取。
     * @param asg    要分析的赋值语句节点 {@link AssignmentNode}。
     */
    @Override
    public void analyze(Context ctx,
                        ModuleInfo mi,
                        FunctionNode fn,
                        SymbolTable locals,
                        AssignmentNode asg) {

        // 获取赋值左值变量名并进行符号解析
        ctx.log("赋值检查: " + asg.variable());
        Symbol sym = locals.resolve(asg.variable());

        // 检查变量是否已声明且为可赋值的变量类型
        if (sym == null || sym.kind() != SymbolKind.VARIABLE) {
            ctx.getErrors().add(new SemanticError(asg,
                    "未声明的变量: " + asg.variable()));
            ctx.log("错误: 未声明的变量 " + asg.variable());
            return;
        }

        // 分析右值表达式类型
        Type valType = ctx.getRegistry().getExpressionAnalyzer(asg.value())
                .analyze(ctx, mi, fn, locals, asg.value());

        // 类型检查：若类型不兼容，则尝试判断是否允许宽化转换
        if (!sym.type().isCompatible(valType)) {
            // 数值类型允许自动宽化转换（如 int → double）
            if (!(sym.type().isNumeric() && valType.isNumeric()
                    && Type.widen(valType, sym.type()) == sym.type())) {
                ctx.getErrors().add(new SemanticError(asg,
                        "赋值类型不匹配: 期望 " + sym.type()
                                + ", 实际 " + valType));
                ctx.log("错误: 赋值类型不匹配 " + asg.variable());
            }
        }
    }
}
