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
 * 该类维护了 AST 节点类型与相应 {@link StatementAnalyzer}、
 * {@link ExpressionAnalyzer} 实例的映射关系。调用者可以通过节点类型注册自定义的分析器，
 * 并在后续通过节点对象高效获取对应分析器，实现语义分析分发。
 * </p>
 * <p>
 * 对于表达式分析器的获取（{@link #getExpressionAnalyzer(ExpressionNode)}），
 * 支持“最近父类匹配”查找机制：若找不到节点的精确类型分析器，则向上递归查找已注册的最近父类类型分析器；
 * 若依然未找到，则自动 fallback 到默认的 {@link UnsupportedExpressionAnalyzer}，确保分析流程健壮性。
 * </p>
 * <p>
 */
public class AnalyzerRegistry {
    /**
     * Statement 节点类型 → 对应语义分析器映射表
     */
    private final Map<Class<? extends StatementNode>, StatementAnalyzer<?>> stmtAnalyzers = new HashMap<>();

    /**
     * Expression 节点类型 → 对应语义分析器映射表
     */
    private final Map<Class<? extends ExpressionNode>, ExpressionAnalyzer<?>> exprAnalyzers = new HashMap<>();

    /**
     * 默认表达式兜底分析器：用于未注册或不能识别的表达式类型
     */
    private final ExpressionAnalyzer<ExpressionNode> defaultUnsupported = new UnsupportedExpressionAnalyzer<>();

    /**
     * 注册 Statement 类型的语义分析器。
     *
     * @param clazz    AST 语句节点类型（class对象），例如 {@code IfStatementNode.class}
     * @param analyzer 针对该节点类型的语义分析器实例
     * @param <S>      语句节点类型参数，必须是 {@link StatementNode} 的子类
     */
    public <S extends StatementNode> void registerStatementAnalyzer(Class<S> clazz, StatementAnalyzer<S> analyzer) {
        stmtAnalyzers.put(clazz, analyzer);
    }

    /**
     * 注册 Expression 类型的语义分析器。
     *
     * @param clazz    AST 表达式节点类型（class对象），例如 {@code BinaryExprNode.class}
     * @param analyzer 针对该节点类型的语义分析器实例
     * @param <E>      表达式节点类型参数，必须是 {@link ExpressionNode} 的子类
     */
    public <E extends ExpressionNode> void registerExpressionAnalyzer(Class<E> clazz, ExpressionAnalyzer<E> analyzer) {
        exprAnalyzers.put(clazz, analyzer);
    }

    /**
     * 获取指定语句节点对象的分析器实例。
     * <p>
     * 只支持“精确类型匹配”，即仅当注册过该节点 class 时才返回分析器，否则返回 null。
     * </p>
     *
     * @param stmt 语句节点对象
     * @param <S>  节点类型，需为 {@link StatementNode} 子类
     * @return 匹配的 {@link StatementAnalyzer}，若未注册则返回 null
     */
    @SuppressWarnings("unchecked")
    public <S extends StatementNode> StatementAnalyzer<S> getStatementAnalyzer(S stmt) {
        return (StatementAnalyzer<S>) stmtAnalyzers.get(stmt.getClass());
    }

    /**
     * 获取指定表达式节点对象的分析器实例。
     * <p>
     * 首先尝试节点类型的精确匹配；若未注册，向上递归查找其最近的已注册父类类型分析器；
     * 若依然未命中，则返回默认的 {@link UnsupportedExpressionAnalyzer}，保证分析器始终有返回值。
     * </p>
     *
     * @param expr 表达式节点对象
     * @param <E>  节点类型，需为 {@link ExpressionNode} 子类
     * @return 匹配的 {@link ExpressionAnalyzer} 实例；若未注册，则返回兜底分析器
     */
    @SuppressWarnings("unchecked")
    public <E extends ExpressionNode> ExpressionAnalyzer<E> getExpressionAnalyzer(E expr) {
        Class<?> cls = expr.getClass();
        // 精确匹配
        ExpressionAnalyzer<?> analyzer = exprAnalyzers.get(cls);
        if (analyzer != null) {
            return (ExpressionAnalyzer<E>) analyzer;
        }
        // 向上遍历父类尝试匹配
        Class<?> current = cls.getSuperclass();
        while (current != null && ExpressionNode.class.isAssignableFrom(current)) {
            analyzer = exprAnalyzers.get(current);
            if (analyzer != null) {
                return (ExpressionAnalyzer<E>) analyzer;
            }
            current = current.getSuperclass();
        }
        // fallback
        return (ExpressionAnalyzer<E>) defaultUnsupported;
    }
}
