package org.jcnc.snow.compiler.ir.builder.statement;

import org.jcnc.snow.compiler.parser.ast.base.StatementNode;

/**
 * 单一语句处理器接口。
 * <p>
 * 定义所有语句处理器的统一标准，用于判断是否能处理特定类型的语句节点，
 * 并在匹配时执行实际的IR构建逻辑。
 * </p>
 * <ul>
 *   <li>每种具体语句类型应有对应的实现类。</li>
 *   <li>用于StatementBuilder的语句分发机制。</li>
 * </ul>
 */
public interface IStatementHandler {
    /**
     * 判断当前处理器是否支持该类型的语句节点。
     *
     * @param stmt 需要判定的AST语句节点。
     * @return 如果支持则返回true，否则返回false。
     */
    boolean canHandle(StatementNode stmt);

    /**
     * 处理指定的语句节点，生成对应的中间代码或IR结构。
     *
     * @param stmt    需处理的AST语句节点。
     * @param context 语句构建上下文（包含IRContext、表达式构建器、控制流等）。
     */
    void handle(StatementNode stmt, StatementBuilderContext context);
}
