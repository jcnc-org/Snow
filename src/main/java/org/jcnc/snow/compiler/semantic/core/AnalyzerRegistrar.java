package org.jcnc.snow.compiler.semantic.core;

import org.jcnc.snow.compiler.parser.ast.*;
import org.jcnc.snow.compiler.semantic.analyzers.AnalyzerRegistry;
import org.jcnc.snow.compiler.semantic.analyzers.expression.*;
import org.jcnc.snow.compiler.semantic.analyzers.statement.*;

/**
 * {@code AnalyzerRegistrar} 负责将所有语句与表达式的语义分析器
 * 统一注册到 {@link AnalyzerRegistry} 中。
 * <p>
 * 本类为静态工具类，不可实例化，其唯一公开方法 {@link #registerAll(AnalyzerRegistry)}
 * 应在语义分析初始化阶段调用一次，确保所有节点类型都能正确分发到对应分析器。
 * <p>
 * 注册内容包括：
 * <ul>
 *   <li>所有标准语句节点（如变量声明、赋值、条件、循环、返回等）的分析器；</li>
 *   <li>所有标准表达式节点（如字面量、标识符、函数调用、二元表达式等）的分析器；</li>
 *   <li>对不支持或未实现的表达式节点提供兜底分析器 {@link UnsupportedExpressionAnalyzer}。</li>
 * </ul>
 */
public final class AnalyzerRegistrar {

    /**
     * 私有构造函数禁止实例化。
     */
    private AnalyzerRegistrar() {
    }

    /**
     * 向指定 {@link AnalyzerRegistry} 注册所有语法分析器实例。
     *
     * @param registry 待注册的分析器注册表实例
     */
    public static void registerAll(AnalyzerRegistry registry) {
        // ---------- 注册语句分析器 ----------
        registry.registerStatementAnalyzer(DeclarationNode.class, new DeclarationAnalyzer());
        registry.registerStatementAnalyzer(AssignmentNode.class, new AssignmentAnalyzer());
        registry.registerStatementAnalyzer(IfNode.class, new IfAnalyzer());
        registry.registerStatementAnalyzer(LoopNode.class, new LoopAnalyzer());
        registry.registerStatementAnalyzer(ReturnNode.class, new ReturnAnalyzer());

        // 特殊处理：表达式语句（如 "foo();"）作为语句包装表达式
        registry.registerStatementAnalyzer(ExpressionStatementNode.class,
                (ctx, mi, fn, locals, stmt) ->
                        registry.getExpressionAnalyzer(stmt.expression())
                                .analyze(ctx, mi, fn, locals, stmt.expression())
        );

        // ---------- 注册表达式分析器 ----------
        registry.registerExpressionAnalyzer(NumberLiteralNode.class, new NumberLiteralAnalyzer());
        registry.registerExpressionAnalyzer(StringLiteralNode.class, new StringLiteralAnalyzer());
        registry.registerExpressionAnalyzer(IdentifierNode.class, new IdentifierAnalyzer());
        registry.registerExpressionAnalyzer(CallExpressionNode.class, new CallExpressionAnalyzer());
        registry.registerExpressionAnalyzer(BinaryExpressionNode.class, new BinaryExpressionAnalyzer());

        // 对尚未实现的表达式类型使用兜底处理器（如 MemberExpression）
        registry.registerExpressionAnalyzer(MemberExpressionNode.class,
                new UnsupportedExpressionAnalyzer<>());
    }
}
