package org.jcnc.snow.compiler.semantic.analyzers.expression;

import org.jcnc.snow.compiler.parser.ast.StringLiteralNode;
import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.semantic.analyzers.base.ExpressionAnalyzer;
import org.jcnc.snow.compiler.semantic.core.Context;
import org.jcnc.snow.compiler.semantic.core.ModuleInfo;
import org.jcnc.snow.compiler.semantic.symbol.SymbolTable;
import org.jcnc.snow.compiler.semantic.type.BuiltinType;
import org.jcnc.snow.compiler.semantic.type.Type;

/**
 * {@code StringLiteralAnalyzer} 是字符串字面量表达式的语义分析器。
 * <p>
 * 负责分析源代码中的字符串字面量（例如 {@code "hello"}、{@code ""} 等），
 * 并确定其类型。根据语言规范，所有字符串字面量默认视为 {@link BuiltinType#STRING} 类型。
 * <p>
 * 特点如下:
 * <ul>
 *   <li>不依赖符号表、函数上下文或模块信息，属于上下文无关表达式分析器；</li>
 *   <li>恒定返回 {@code STRING} 类型；</li>
 *   <li>不产生任何语义错误，具备语义稳定性。</li>
 * </ul>
 */
public class StringLiteralAnalyzer implements ExpressionAnalyzer<StringLiteralNode> {

    /**
     * 分析字符串字面量表达式并返回其类型。
     *
     * @param ctx    当前语义分析上下文对象（本分析器不使用该参数，保留用于接口一致性）
     * @param mi     当前模块信息（未使用，因字面量无模块依赖）
     * @param fn     当前所在的函数节点（未使用，因字面量无函数依赖）
     * @param locals 当前作用域的符号表（未使用，因字面量不引用符号）
     * @param expr   要分析的 {@link StringLiteralNode} 节点
     * @return 始终返回 {@link BuiltinType#STRING} 类型，表示字符串字面量
     */
    @Override
    public Type analyze(Context ctx,
                        ModuleInfo mi,
                        FunctionNode fn,
                        SymbolTable locals,
                        StringLiteralNode expr) {
        return BuiltinType.STRING;
    }
}
