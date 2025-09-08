package org.jcnc.snow.compiler.semantic.analyzers.expression;

import org.jcnc.snow.compiler.parser.ast.BinaryExpressionNode;
import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.semantic.analyzers.base.ExpressionAnalyzer;
import org.jcnc.snow.compiler.semantic.core.Context;
import org.jcnc.snow.compiler.semantic.core.ModuleInfo;
import org.jcnc.snow.compiler.semantic.error.SemanticError;
import org.jcnc.snow.compiler.semantic.symbol.SymbolTable;
import org.jcnc.snow.compiler.semantic.type.BuiltinType;
import org.jcnc.snow.compiler.semantic.type.Type;

/**
 * {@code BinaryExpressionAnalyzer} 负责对二元表达式做语义分析并返回其类型。
 * <p>
 * 支持特性:
 * 1. 字符串拼接「+」
 * 2. 数值运算与自动宽化
 * 3. 比较 / 关系运算
 * 4. 布尔值比较（== / !=）
 * <p>
 * 如遇不支持的运算符或不兼容的类型组合，将记录语义错误，
 * 并以 {@code int} 作为回退类型，保持后续分析不中断。
 */
public class BinaryExpressionAnalyzer implements ExpressionAnalyzer<BinaryExpressionNode> {

    @Override
    public Type analyze(Context ctx,
                        ModuleInfo mi,
                        FunctionNode fn,
                        SymbolTable locals,
                        BinaryExpressionNode bin) {

        ctx.log("检查二元表达式: " + bin.operator());

        /* ----------- 先递归分析左右子表达式类型 ----------- */
        Type left = ctx.getRegistry()
                .getExpressionAnalyzer(bin.left())
                .analyze(ctx, mi, fn, locals, bin.left());

        Type right = ctx.getRegistry()
                .getExpressionAnalyzer(bin.right())
                .analyze(ctx, mi, fn, locals, bin.right());

        String op = bin.operator();

        /* ----------- 情况 1: 字符串拼接 ----------- */
        if (op.equals("+") &&
                (left == BuiltinType.STRING || right == BuiltinType.STRING)) {
            return BuiltinType.STRING;
        }

        /* ----------- 情况 1.5: 布尔值比较 ----------- */
        if (("==".equals(op) || "!=".equals(op)) &&
                left == BuiltinType.BOOLEAN &&
                right == BuiltinType.BOOLEAN) {
            return BuiltinType.BOOLEAN;
        }

        /* ----------- 情况 2: 数值运算 / 比较 ----------- */
        if ("+-*/%".contains(op) || ("<<=>>===!=").contains(op)) {
            if (left.isNumeric() && right.isNumeric()) {
                // 自动数值宽化（如 int + float → float）
                Type wide = Type.widen(left, right);
                if (wide == null) wide = BuiltinType.INT;   // 容错降级

                // 比较运算统一返回 boolean
                if ("< <= > >= == !=".contains(op)) {
                    return BuiltinType.BOOLEAN;
                }
                return wide;
            }
        }

        /* ----------- 情况 3: 不支持的类型组合 ----------- */
        ctx.getErrors().add(new SemanticError(
                bin,
                String.format("运算符 '%s' 不支持类型: %s 和 %s", op, left, right)
        ));
        ctx.log("错误: 运算符 '" + op + "' 不支持类型: " + left + ", " + right);

        // 回退类型
        return BuiltinType.INT;
    }
}
