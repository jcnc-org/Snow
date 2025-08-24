package org.jcnc.snow.compiler.semantic.analyzers.expression;

import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.parser.ast.IndexExpressionNode;
import org.jcnc.snow.compiler.semantic.analyzers.base.ExpressionAnalyzer;
import org.jcnc.snow.compiler.semantic.core.Context;
import org.jcnc.snow.compiler.semantic.core.ModuleInfo;
import org.jcnc.snow.compiler.semantic.error.SemanticError;
import org.jcnc.snow.compiler.semantic.symbol.SymbolTable;
import org.jcnc.snow.compiler.semantic.type.ArrayType;
import org.jcnc.snow.compiler.semantic.type.BuiltinType;
import org.jcnc.snow.compiler.semantic.type.Type;

/**
 * {@code IndexExpressionAnalyzer} 负责下标访问表达式（如 {@code array[index]}）的类型推断和检查。
 * <p>
 * 语义规则：
 * <ol>
 *   <li>先分析 array 的类型，必须为 {@link ArrayType}</li>
 *   <li>再分析 index 的类型，必须为数值类型</li>
 *   <li>表达式结果类型为 array 的元素类型；若 array 非数组类型，报错并返回 int 类型兜底</li>
 * </ol>
 */
public class IndexExpressionAnalyzer implements ExpressionAnalyzer<IndexExpressionNode> {

    /**
     * 分析并类型检查下标访问表达式。
     *
     * @param ctx    当前语义分析上下文
     * @param mi     当前模块信息
     * @param fn     当前函数节点
     * @param locals 当前作用域符号表
     * @param node   要分析的下标访问表达式节点
     * @return 若 array 合法，则返回元素类型；否则兜底为 int 类型
     */
    @Override
    public Type analyze(Context ctx, ModuleInfo mi, FunctionNode fn, SymbolTable locals, IndexExpressionNode node) {
        // 先分析被下标访问的数组表达式
        Type arrType = ctx.getRegistry()
                .getExpressionAnalyzer(node.array())
                .analyze(ctx, mi, fn, locals, node.array());

        if (arrType instanceof ArrayType(Type elementType)) {
            // 再分析下标表达式
            Type idxType = ctx.getRegistry()
                    .getExpressionAnalyzer(node.index())
                    .analyze(ctx, mi, fn, locals, node.index());

            if (!idxType.isNumeric()) {
                ctx.getErrors().add(new SemanticError(node, "数组下标必须是数值类型"));
            }
            // array[index] 的类型是数组元素类型
            return elementType;
        }

        ctx.getErrors().add(new SemanticError(node, "仅数组类型支持下标访问"));
        return BuiltinType.INT;
    }
}
