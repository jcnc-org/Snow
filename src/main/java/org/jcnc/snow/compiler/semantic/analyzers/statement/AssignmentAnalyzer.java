package org.jcnc.snow.compiler.semantic.analyzers.statement;

import org.jcnc.snow.compiler.parser.ast.AssignmentNode;
import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.semantic.analyzers.base.StatementAnalyzer;
import org.jcnc.snow.compiler.semantic.core.Context;
import org.jcnc.snow.compiler.semantic.core.ModuleInfo;
import org.jcnc.snow.compiler.semantic.error.SemanticError;
import org.jcnc.snow.compiler.semantic.symbol.Symbol;
import org.jcnc.snow.compiler.semantic.symbol.SymbolKind;
import org.jcnc.snow.compiler.semantic.symbol.SymbolTable;
import org.jcnc.snow.compiler.semantic.type.Type;
import org.jcnc.snow.compiler.semantic.utils.NumericConstantUtils;

/**
 * {@code AssignmentAnalyzer} 负责对赋值语句进行语义校验。
 *
 * <h2>校验要点</h2>
 * <ul>
 *   <li><b>左值解析</b>：确保标识符已声明；</li>
 *   <li><b>常量保护</b>：禁止修改 {@link SymbolKind#CONSTANT}；</li>
 *   <li><b>类型兼容</b>：验证右值类型与目标类型兼容，若均为数值类型则允许宽化转换（如 <code>int → double</code>）。</li>
 * </ul>
 * <p>
 * 任何不满足条件的情况都会向 {@link Context#getErrors()} 记录 {@link SemanticError}。
 */
public class AssignmentAnalyzer implements StatementAnalyzer<AssignmentNode> {

    /**
     * 错误消息前缀，统一便于搜索定位
     */
    private static final String ERR_PREFIX = "赋值错误: ";

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
        if (sym == null) {
            ctx.getErrors().add(new SemanticError(asg,
                    ERR_PREFIX + "未声明的变量: " + asg.variable()));
            ctx.log(ERR_PREFIX + "未声明的变量 " + asg.variable());
            return; // 无需继续后续检查
        }

        /* ---------- 2. 常量不可修改 ---------- */
        if (sym.kind() == SymbolKind.CONSTANT) {
            ctx.getErrors().add(new SemanticError(asg,
                    ERR_PREFIX + "无法修改常量: " + asg.variable()));
            ctx.log(ERR_PREFIX + "尝试修改常量 " + asg.variable());
            return;
        }

        /* ---------- 3. 右值类型分析 ---------- */
        Type rhsType = ctx.getRegistry()
                .getExpressionAnalyzer(asg.value())
                .analyze(ctx, mi, fn, locals, asg.value());

        /* ---------- 4. 类型兼容性检查 ---------- */
        boolean compatible = sym.type().isCompatible(rhsType);
        boolean widenOK = sym.type().isNumeric()
                && rhsType.isNumeric()
                && Type.widen(rhsType, sym.type()) == sym.type();
        boolean narrowingConst = NumericConstantUtils.canNarrowToIntegral(sym.type(), rhsType, asg.value());

        if (!compatible && !widenOK && !narrowingConst) {
            ctx.getErrors().add(new SemanticError(asg,
                    ERR_PREFIX + "类型不匹配: 期望 "
                            + sym.type() + ", 实际 " + rhsType));
            ctx.log(ERR_PREFIX + "类型不匹配 " + asg.variable());
        }
    }
}
