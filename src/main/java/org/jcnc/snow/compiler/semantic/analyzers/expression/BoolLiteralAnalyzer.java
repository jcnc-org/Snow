package org.jcnc.snow.compiler.semantic.analyzers.expression;

import org.jcnc.snow.compiler.parser.ast.BoolLiteralNode;
import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.semantic.analyzers.base.ExpressionAnalyzer;
import org.jcnc.snow.compiler.semantic.core.Context;
import org.jcnc.snow.compiler.semantic.core.ModuleInfo;
import org.jcnc.snow.compiler.semantic.symbol.SymbolTable;
import org.jcnc.snow.compiler.semantic.type.BuiltinType;
import org.jcnc.snow.compiler.semantic.type.Type;

/**
 * {@code BoolLiteralAnalyzer} 是布尔字面量表达式的语义分析器。
 * <p>
 * 本类实现 {@link ExpressionAnalyzer} 接口，用于在语义分析阶段对 {@link BoolLiteralNode}
 * 进行类型推断和校验。在此实现中，所有布尔字面量表达式都被直接视为内建布尔类型 {@link BuiltinType#BOOLEAN}。
 * </p>
 * <p>
 * 该分析器不涉及值检查，仅负责返回类型信息，用于后续的类型检查与代码生成阶段。
 * </p>
 */
public class BoolLiteralAnalyzer implements ExpressionAnalyzer<BoolLiteralNode> {

    /**
     * 分析布尔字面量表达式的语义，并返回其类型。
     * <p>
     * 由于布尔字面量具有确定且固定的类型，本方法始终返回 {@link BuiltinType#BOOLEAN}。
     * </p>
     *
     * @param ctx    当前的语义分析上下文，包含全局编译状态
     * @param mi     所在模块的信息对象
     * @param fn     当前分析所在的函数节点
     * @param locals 当前作用域下的符号表
     * @param expr   被分析的布尔字面量表达式节点
     * @return {@link BuiltinType#BOOLEAN}，表示布尔类型
     */
    @Override
    public Type analyze(Context ctx, ModuleInfo mi, FunctionNode fn,
                        SymbolTable locals, BoolLiteralNode expr) {
        return BuiltinType.BOOLEAN;
    }
}
