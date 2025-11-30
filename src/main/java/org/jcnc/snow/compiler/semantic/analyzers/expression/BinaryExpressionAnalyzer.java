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
        /* ----------- 情况 1.6: 字符串比较 ----------- */
        if (("==".equals(op) || "!=".equals(op)) &&
                left == BuiltinType.STRING &&
                right == BuiltinType.STRING) {
            return BuiltinType.BOOLEAN;
        }
        /* ----------- 情况 1.7: 布尔逻辑运算 ----------- */
        if (("&&".equals(op) || "||".equals(op)) &&
                left == BuiltinType.BOOLEAN &&
                right == BuiltinType.BOOLEAN) {
            return BuiltinType.BOOLEAN;
        }

        /* ----------- 情况 2: 数值运算 / 比较 ----------- */
        boolean isComparison = op.equals("<") || op.equals("<=")
                || op.equals(">") || op.equals(">=")
                || op.equals("==") || op.equals("!=");
        boolean isNumericOp = "+-*/%".contains(op) || op.equals("<<") || op.equals(">>") || isComparison;
        if (isNumericOp && left.isNumeric() && right.isNumeric()) {
            Type wide = promoteNumeric(left, right);
            if (wide == null) wide = BuiltinType.INT;   // 容错降级

            // 比较运算统一返回 boolean
            if (isComparison) {
                return BuiltinType.BOOLEAN;
            }
            return wide;
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

    /**
     * 二元数值运算的结果类型推断：
     * double > float > long > int（byte/short 会先提升为 int）。
     */
    private static Type promoteNumeric(Type left, Type right) {
        if (!(left.isNumeric() && right.isNumeric())) return null;
        if (left == BuiltinType.DOUBLE || right == BuiltinType.DOUBLE) return BuiltinType.DOUBLE;
        if (left == BuiltinType.FLOAT || right == BuiltinType.FLOAT) return BuiltinType.FLOAT;
        if (left == BuiltinType.LONG || right == BuiltinType.LONG) return BuiltinType.LONG;
        return BuiltinType.INT;
    }
}
