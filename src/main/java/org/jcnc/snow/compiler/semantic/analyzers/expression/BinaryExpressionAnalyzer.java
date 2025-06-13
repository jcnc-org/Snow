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
 * {@code BinaryExpressionAnalyzer} 是一个用于分析二元表达式的语义分析器。
 * <p>
 * 支持的特性包括：
 * <ul>
 *     <li>字符串拼接（当运算符为加号 "+" 且任一操作数为字符串类型时）</li>
 *     <li>数值类型的自动宽化转换（如 int 与 float 运算将转换为 float）</li>
 *     <li>基本的数值运算符（如 +, -, *, /, %）</li>
 *     <li>关系运算符和比较运算符（如 <, <=, >, >=, ==, !=）</li>
 * </ul>
 * 对于不支持的运算符或不兼容的类型组合，将记录语义错误，并默认返回 {@code BuiltinType.INT} 以保持分析过程的连续性。
 * <p>
 * 实现类遵循 {@code ExpressionAnalyzer<BinaryExpressionNode>} 接口规范。
 */
public class BinaryExpressionAnalyzer implements ExpressionAnalyzer<BinaryExpressionNode> {

    /**
     * 分析给定的二元表达式节点，返回其表达式类型。
     *
     * @param ctx    当前语义分析上下文，用于访问日志记录、错误收集、注册表等服务。
     * @param mi     当前模块信息，包含模块级别的符号与类型定义。
     * @param fn     当前正在分析的函数节点。
     * @param locals 当前函数作用域内的符号表，用于变量查找。
     * @param bin    要分析的二元表达式节点。
     * @return 分析后推断出的表达式类型。
     */
    @Override
    public Type analyze(Context ctx,
                        ModuleInfo mi,
                        FunctionNode fn,
                        SymbolTable locals,
                        BinaryExpressionNode bin) {
        ctx.log("检查二元表达式: " + bin.operator());

        // 获取左侧表达式类型
        Type left = ctx.getRegistry().getExpressionAnalyzer(bin.left())
                .analyze(ctx, mi, fn, locals, bin.left());

        // 获取右侧表达式类型
        Type right = ctx.getRegistry().getExpressionAnalyzer(bin.right())
                .analyze(ctx, mi, fn, locals, bin.right());

        String op = bin.operator();

        // 情况 1：字符串拼接（+ 操作符且任一操作数为字符串类型）
        if (op.equals("+") &&
                (left == BuiltinType.STRING || right == BuiltinType.STRING)) {
            return BuiltinType.STRING;
        }

        // 情况 2：数值类型运算或比较
        if ("+-*/%".contains(op) || ("<<=>>===!=").contains(op)) {
            if (left.isNumeric() && right.isNumeric()) {
                // 自动宽化到更宽的数值类型（如 int + float => float）
                Type wide = Type.widen(left, right);
                if (wide == null) wide = BuiltinType.INT; // 容错降级为 int

                // 若为比较运算符，统一返回 int 类型作为布尔值表示
                if ("< <= > >= == !=".contains(op)) {
                    return BuiltinType.BOOLEAN;
                }

                return wide;
            }
        }

        // 情况 3：不支持的类型组合，记录语义错误
        ctx.getErrors().add(new SemanticError(
                bin,
                String.format("运算符 '%s' 不支持类型: %s 和 %s", op, left, right)
        ));
        ctx.log("错误: 运算符 '" + op + "' 不支持类型: " + left + ", " + right);

        // 错误情况下默认返回 int 类型，以保证语义分析不中断
        return BuiltinType.INT;
    }
}
