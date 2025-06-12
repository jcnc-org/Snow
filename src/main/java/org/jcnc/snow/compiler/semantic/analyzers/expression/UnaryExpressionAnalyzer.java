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

/** 一元表达式语义分析 */
public class UnaryExpressionAnalyzer implements ExpressionAnalyzer<UnaryExpressionNode> {

    @Override
    public Type analyze(Context ctx, ModuleInfo mi, FunctionNode fn,
                        SymbolTable locals, UnaryExpressionNode expr) {

        // 先分析操作数
        Type operandType = ctx.getRegistry()
                              .getExpressionAnalyzer(expr.operand())
                              .analyze(ctx, mi, fn, locals, expr.operand());

        switch (expr.operator()) {
            case "-" -> {
                if (!operandType.isNumeric()) {
                    ctx.getErrors().add(new SemanticError(expr,
                            "'-' 只能应用于数值类型，当前为 " + operandType));
                    return BuiltinType.INT;
                }
                return operandType;
            }
            case "!" -> {
                if (operandType != BuiltinType.BOOLEAN) {
                    ctx.getErrors().add(new SemanticError(expr,
                            "'!' 只能应用于 boolean 类型，当前为 " + operandType));
                    return BuiltinType.BOOLEAN;
                }
                return BuiltinType.BOOLEAN;
            }
            default -> {
                ctx.getErrors().add(new SemanticError(expr,
                        "未知一元运算符: " + expr.operator()));
                return BuiltinType.INT;
            }
        }
    }
}
