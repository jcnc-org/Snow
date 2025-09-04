package org.jcnc.snow.compiler.ir.builder.statement;

import org.jcnc.snow.compiler.ir.builder.core.IRContext;
import org.jcnc.snow.compiler.ir.builder.expression.ExpressionBuilder;
import org.jcnc.snow.compiler.ir.builder.statement.handlers.*;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 语句构建总调度器。
 * <p>
 * 根据AST语句类型自动选择并调度对应的语句处理器（StatementHandler），
 * 负责完成语句节点到IR层中间代码的构建。
 * </p>
 * <ul>
 *   <li>内部维护一组支持的语句处理器，按顺序优先匹配。</li>
 *   <li>支持表达式、赋值、声明、流程控制、返回、break/continue等常见语句类型。</li>
 *   <li>支持嵌套语句、循环、条件、块等场景的递归构建。</li>
 * </ul>
 */
public class StatementBuilder {

    /**
     * 当前IR上下文，贯穿所有构建过程。
     */
    private final IRContext ctx;

    /**
     * 所有已注册的语句处理器（顺序很重要，先匹配更具体的）。
     */
    private final List<IStatementHandler> handlers = new ArrayList<>();

    /**
     * 内部构建过程上下文，封装递归与辅助对象。
     */
    private final StatementBuilderContext context;

    /**
     * 创建新的语句构建调度器。
     *
     * @param ctx IR上下文对象，保存符号表、中间代码、标签等状态。
     */
    public StatementBuilder(IRContext ctx) {
        this.ctx = ctx;
        ExpressionBuilder expr = new ExpressionBuilder(ctx);

        // 回调：允许处理器在嵌套处继续调用“顶级 build”
        Consumer<StatementNode> buildOne = this::build;
        Consumer<Iterable<StatementNode>> buildMany = this::buildStatements;

        // 控制流用：记录break与continue跳转目标（支持嵌套循环/块）
        ArrayDeque<String> breakTargets = new ArrayDeque<>();
        ArrayDeque<String> continueTargets = new ArrayDeque<>();
        this.context = new StatementBuilderContext(
                ctx, expr, breakTargets, continueTargets, buildOne, buildMany
        );

        // 注册所有支持的语句处理器（顺序不能随意调整）
        handlers.add(new LoopHandler());
        handlers.add(new IfHandler());
        handlers.add(new ExpressionStmtHandler());
        handlers.add(new AssignmentHandler());
        handlers.add(new IndexAssignmentHandler());
        handlers.add(new DeclarationHandler());
        handlers.add(new ReturnHandler());
        handlers.add(new BreakHandler());
        handlers.add(new ContinueHandler());
    }

    /**
     * 构建单条语句：根据语句类型自动分发至合适的处理器。
     *
     * @param stmt AST语句节点（StatementNode）。
     * @throws IllegalStateException 如果未找到任何合适的处理器。
     */
    public void build(StatementNode stmt) {
        for (IStatementHandler h : handlers) {
            if (h.canHandle(stmt)) {
                h.handle(stmt, context);
                return;
            }
        }
        throw new IllegalStateException("Unsupported statement: "
                + stmt.getClass().getSimpleName() + ": " + stmt);
    }

    /**
     * 批量构建一组语句（如代码块、函数体等）。
     *
     * @param stmts AST语句节点集合。
     */
    private void buildStatements(Iterable<StatementNode> stmts) {
        for (StatementNode s : stmts) build(s);
    }

    /**
     * 获取IR上下文对象（只读）。
     *
     * @return 当前IRContext。
     */
    public IRContext getCtx() {
        return ctx;
    }
}
