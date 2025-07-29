package org.jcnc.snow.compiler.semantic.analyzers.statement;

import org.jcnc.snow.compiler.parser.ast.BreakNode;
import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.semantic.analyzers.base.StatementAnalyzer;
import org.jcnc.snow.compiler.semantic.core.Context;
import org.jcnc.snow.compiler.semantic.core.ModuleInfo;
import org.jcnc.snow.compiler.semantic.symbol.SymbolTable;

/**
 * {@code BreakAnalyzer} —— break 语句的语义分析器。
 * <p>
 * 目前不做额外检查；是否位于循环体内的限制由 IR 构建阶段（StatementBuilder）
 * 进行安全校验（若出现在循环外，会抛出清晰的错误）。
 * 如需在语义阶段提前报错，可在此处结合“循环上下文”进行校验。
 */
public class BreakAnalyzer implements StatementAnalyzer<BreakNode> {

    @Override
    public void analyze(Context ctx,
                        ModuleInfo mi,
                        FunctionNode fn,
                        SymbolTable locals,
                        BreakNode stmt) {
        // no-op: break 本身不引入新符号或类型约束
        // 在语义阶段校验“是否处于循环内”，需要在 ctx 或 SymbolTable 上增加上下文标记后在此检查。
    }
}
