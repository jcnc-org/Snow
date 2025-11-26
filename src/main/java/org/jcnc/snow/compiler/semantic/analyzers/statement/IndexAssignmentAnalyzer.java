package org.jcnc.snow.compiler.semantic.analyzers.statement;

import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.parser.ast.IndexAssignmentNode;
import org.jcnc.snow.compiler.parser.ast.IndexExpressionNode;
import org.jcnc.snow.compiler.semantic.analyzers.base.StatementAnalyzer;
import org.jcnc.snow.compiler.semantic.core.Context;
import org.jcnc.snow.compiler.semantic.core.ModuleInfo;
import org.jcnc.snow.compiler.semantic.error.SemanticError;
import org.jcnc.snow.compiler.semantic.symbol.SymbolTable;
import org.jcnc.snow.compiler.semantic.type.ArrayType;
import org.jcnc.snow.compiler.semantic.type.Type;
import org.jcnc.snow.compiler.semantic.utils.NumericConstantUtils;

/**
 * {@code IndexAssignmentAnalyzer} 用于分析和类型检查数组下标赋值语句。
 * <p>
 * 主要职责：
 * <ul>
 *   <li>检查左侧是否为数组类型</li>
 *   <li>检查下标是否为数值类型</li>
 *   <li>检查右值（被赋值表达式）类型是否与数组元素类型兼容</li>
 *   <li>如有类型不符，将语义错误写入 {@link Context#getErrors()}</li>
 * </ul>
 */
public class IndexAssignmentAnalyzer implements StatementAnalyzer<IndexAssignmentNode> {

    /**
     * 分析并类型检查数组下标赋值语句，如 {@code arr[i] = v}。
     * <ul>
     *   <li>左侧必须是数组类型</li>
     *   <li>下标必须为数值类型</li>
     *   <li>右侧赋值类型需与数组元素类型一致</li>
     *   <li>任何不合法情况均会产生 {@link SemanticError}</li>
     * </ul>
     *
     * @param ctx    当前语义分析上下文
     * @param mi     当前模块信息
     * @param fn     所属函数节点
     * @param locals 局部符号表
     * @param node   赋值语句 AST 节点
     */
    @Override
    public void analyze(Context ctx, ModuleInfo mi, FunctionNode fn, SymbolTable locals, IndexAssignmentNode node) {
        IndexExpressionNode target = node.target();

        // 分析左侧类型（必须为数组类型）
        Type arrT = ctx.getRegistry()
                .getExpressionAnalyzer(target.array())
                .analyze(ctx, mi, fn, locals, target.array());
        if (!(arrT instanceof ArrayType(Type elementType))) {
            ctx.getErrors().add(new SemanticError(node, "左侧不是数组，无法进行下标赋值"));
            return;
        }

        // 分析下标类型（必须为数值类型）
        Type idxT = ctx.getRegistry()
                .getExpressionAnalyzer(target.index())
                .analyze(ctx, mi, fn, locals, target.index());
        if (!idxT.isNumeric()) {
            ctx.getErrors().add(new SemanticError(node, "数组下标必须是数值类型"));
        }

        // 分析右值类型（必须与元素类型一致）
        Type rhsT = ctx.getRegistry()
                .getExpressionAnalyzer(node.value())
                .analyze(ctx, mi, fn, locals, node.value());
        boolean compatible = elementType.isCompatible(rhsT);
        boolean widenOK = elementType.isNumeric()
                && rhsT.isNumeric()
                && Type.widen(rhsT, elementType) == elementType;
        boolean narrowingConst = NumericConstantUtils.canNarrowToIntegral(elementType, rhsT, node.value());

        if (!compatible && !widenOK && !narrowingConst) {
            ctx.getErrors().add(new SemanticError(node,
                    "数组元素赋值类型不匹配: 期望 " + elementType.name() + "，实际 " + rhsT.name()));
        }
    }
}
