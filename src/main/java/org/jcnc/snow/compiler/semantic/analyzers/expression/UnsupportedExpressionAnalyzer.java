package org.jcnc.snow.compiler.semantic.analyzers.expression;

import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.semantic.analyzers.base.ExpressionAnalyzer;
import org.jcnc.snow.compiler.semantic.core.Context;
import org.jcnc.snow.compiler.semantic.core.ModuleInfo;
import org.jcnc.snow.compiler.semantic.error.SemanticError;
import org.jcnc.snow.compiler.semantic.symbol.SymbolTable;
import org.jcnc.snow.compiler.semantic.type.BuiltinType;
import org.jcnc.snow.compiler.semantic.type.Type;

/**
 * {@code UnsupportedExpressionAnalyzer} 是一个通用兜底表达式分析器，
 * 用于处理所有未显式注册的 {@link ExpressionNode} 子类型。
 * <p>
 * 在语义分析阶段，当分析器注册表中找不到对应节点类型的处理器时，
 * 将回退使用本类进行统一处理，以确保编译流程不中断。
 * <p>
 * 特性说明:
 * <ul>
 *   <li>适用于所有未知或暂未实现的表达式类型；</li>
 *   <li>自动记录语义错误并打印日志，方便定位与扩展；</li>
 *   <li>返回统一的降级类型 {@link BuiltinType#INT}，以避免类型缺失造成后续分析失败。</li>
 * </ul>
 *
 * @param <E> 任意 {@link ExpressionNode} 的子类，支持泛型兜底匹配
 */
public class UnsupportedExpressionAnalyzer<E extends ExpressionNode>
        implements ExpressionAnalyzer<E> {

    /**
     * 对不支持或未实现的表达式节点执行兜底处理。
     *
     * @param ctx    当前语义分析上下文对象，用于错误记录与日志输出
     * @param mi     当前模块信息（此方法中未使用，保留用于接口一致性）
     * @param fn     当前函数节点（此方法中未使用，保留用于接口一致性）
     * @param locals 当前局部作用域符号表（此方法中未使用，因不解析具体含义）
     * @param expr   不支持的表达式节点
     * @return 固定返回 {@link BuiltinType#INT} 类型，作为占位降级类型
     */
    @Override
    public Type analyze(Context ctx,
                        ModuleInfo mi,
                        FunctionNode fn,
                        SymbolTable locals,
                        E expr) {
        // 记录语义错误
        ctx.getErrors().add(new SemanticError(
                expr,
                "不支持的表达式类型: " + expr
        ));

        // 输出错误日志以便调试或后续支持扩展
        ctx.log("错误: 不支持的表达式类型 " + expr);

        // 返回默认类型以避免连锁报错
        return BuiltinType.INT;
    }
}
