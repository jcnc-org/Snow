package org.jcnc.snow.compiler.semantic.analyzers.expression;

import org.jcnc.snow.compiler.parser.ast.ArrayLiteralNode;
import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.semantic.analyzers.base.ExpressionAnalyzer;
import org.jcnc.snow.compiler.semantic.core.Context;
import org.jcnc.snow.compiler.semantic.core.ModuleInfo;
import org.jcnc.snow.compiler.semantic.error.SemanticError;
import org.jcnc.snow.compiler.semantic.symbol.SymbolTable;
import org.jcnc.snow.compiler.semantic.type.ArrayType;
import org.jcnc.snow.compiler.semantic.type.BuiltinType;
import org.jcnc.snow.compiler.semantic.type.Type;

import java.util.List;

/**
 * {@code ArrayLiteralAnalyzer} 用于分析数组字面量表达式（ArrayLiteralNode）。
 * <p>
 * 主要负责：
 * <ul>
 *     <li>推断数组字面量的元素类型，生成对应的 {@link ArrayType}。</li>
 *     <li>检查所有元素类型是否一致，不一致时报错并降级为 {@code int[]}。</li>
 *     <li>若数组为空，默认类型为 {@code int[]}，并产生相应语义错误。</li>
 * </ul>
 * </p>
 */
public class ArrayLiteralAnalyzer implements ExpressionAnalyzer<ArrayLiteralNode> {
    /**
     * 分析数组字面量表达式，推断类型，并检查元素类型一致性。
     *
     * @param ctx    全局/当前语义分析上下文
     * @param mi     所属模块信息
     * @param fn     当前分析的函数节点
     * @param locals 当前作用域符号表
     * @param expr   当前数组字面量节点
     * @return 推断出的数组类型，若类型冲突或无法推断，则为 {@code int[]}
     */
    @Override
    public Type analyze(Context ctx, ModuleInfo mi, FunctionNode fn, SymbolTable locals, ArrayLiteralNode expr) {
        List<ExpressionNode> elems = expr.elements();
        if (elems.isEmpty()) {
            ctx.getErrors().add(new SemanticError(expr, "空数组字面量的类型无法推断，已默认为 int[]"));
            return new ArrayType(BuiltinType.INT);
        }
        // 以第一个元素为基准
        Type first = ctx.getRegistry()
                .getExpressionAnalyzer(elems.getFirst())
                .analyze(ctx, mi, fn, locals, elems.getFirst());

        for (int i = 1; i < elems.size(); i++) {
            ExpressionNode e = elems.get(i);
            Type t = ctx.getRegistry()
                    .getExpressionAnalyzer(e)
                    .analyze(ctx, mi, fn, locals, e);
            if (!t.equals(first)) {
                ctx.getErrors().add(new SemanticError(e, "数组元素类型不一致: 期望 " + first.name() + "，实际 " + t.name()));
                return new ArrayType(BuiltinType.INT);
            }
        }
        return new ArrayType(first);
    }
}
