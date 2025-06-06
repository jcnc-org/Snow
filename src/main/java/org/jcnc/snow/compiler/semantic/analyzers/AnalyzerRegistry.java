package org.jcnc.snow.compiler.semantic.analyzers;

import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;
import org.jcnc.snow.compiler.semantic.analyzers.base.ExpressionAnalyzer;
import org.jcnc.snow.compiler.semantic.analyzers.base.StatementAnalyzer;
import org.jcnc.snow.compiler.semantic.analyzers.expression.UnsupportedExpressionAnalyzer;

import java.util.HashMap;
import java.util.Map;

/**
 * {@code AnalyzerRegistry} 是语义分析器的注册与分发中心。
 * <p>
 * 它负责根据 AST 节点的类型，查找并返回相应的 {@link StatementAnalyzer} 或 {@link ExpressionAnalyzer} 实例。
 * 同时支持注册自定义分析器，并在未找到对应表达式分析器时提供默认兜底处理器。
 * <p>
 * 主要职责：
 * <ul>
 *   <li>支持注册语句和表达式节点类型对应的分析器；</li>
 *   <li>在语义分析阶段，根据 AST 节点动态查找对应的分析器；</li>
 *   <li>为未注册的表达式类型提供默认处理器 {@link UnsupportedExpressionAnalyzer}；</li>
 *   <li>不为语句提供默认兜底分析器，未注册类型将返回 {@code null}。</li>
 * </ul>
 */
public class AnalyzerRegistry {
    /** Statement 节点类型 → 对应语义分析器映射表 */
    private final Map<Class<?>, StatementAnalyzer<?>> stmtAnalyzers = new HashMap<>();

    /** Expression 节点类型 → 对应语义分析器映射表 */
    private final Map<Class<?>, ExpressionAnalyzer<?>> exprAnalyzers = new HashMap<>();

    /** 默认兜底表达式分析器，用于处理未注册的表达式类型 */
    private final ExpressionAnalyzer<ExpressionNode> defaultUnsupported =
            new UnsupportedExpressionAnalyzer<>();

    // ========================= 注册方法 =========================

    /**
     * 注册一个 {@link StatementAnalyzer} 实例，用于处理指定类型的语句节点。
     *
     * @param cls      要注册的语句节点类型（Class 对象）
     * @param analyzer 与该类型匹配的分析器实例
     * @param <S>      {@link StatementNode} 的具体子类
     */
    public <S extends StatementNode> void registerStatementAnalyzer(
            Class<S> cls,
            StatementAnalyzer<S> analyzer
    ) {
        stmtAnalyzers.put(cls, analyzer);
    }

    /**
     * 注册一个 {@link ExpressionAnalyzer} 实例，用于处理指定类型的表达式节点。
     *
     * @param cls      要注册的表达式节点类型（Class 对象）
     * @param analyzer 与该类型匹配的分析器实例
     * @param <E>      {@link ExpressionNode} 的具体子类
     */
    public <E extends ExpressionNode> void registerExpressionAnalyzer(
            Class<E> cls,
            ExpressionAnalyzer<E> analyzer
    ) {
        exprAnalyzers.put(cls, analyzer);
    }

    // ========================= 获取方法 =========================

    /**
     * 根据语句节点的实际类型查找对应的 {@link StatementAnalyzer}。
     * <p>
     * 若节点类型未注册，返回 {@code null}。
     *
     * @param stmt 要分析的语句节点实例
     * @param <S>  语句类型（推断自参数）
     * @return 与该节点类型对应的分析器，若未注册则为 {@code null}
     */
    @SuppressWarnings("unchecked")
    public <S extends StatementNode> StatementAnalyzer<S> getStatementAnalyzer(S stmt) {
        return (StatementAnalyzer<S>) stmtAnalyzers.get(stmt.getClass());
    }

    /**
     * 根据表达式节点的实际类型查找对应的 {@link ExpressionAnalyzer}。
     * <p>
     * 若节点类型未注册，返回默认兜底分析器 {@link UnsupportedExpressionAnalyzer}。
     *
     * @param expr 要分析的表达式节点实例
     * @param <E>  表达式类型（推断自参数）
     * @return 与该节点类型对应的分析器，或默认兜底分析器
     */
    @SuppressWarnings("unchecked")
    public <E extends ExpressionNode> ExpressionAnalyzer<E> getExpressionAnalyzer(E expr) {
        return (ExpressionAnalyzer<E>)
                exprAnalyzers.getOrDefault(expr.getClass(), defaultUnsupported);
    }
}
