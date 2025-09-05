package org.jcnc.snow.compiler.ir.builder.statement.handlers;

import org.jcnc.snow.compiler.ir.builder.statement.IStatementHandler;
import org.jcnc.snow.compiler.ir.builder.statement.StatementBuilderContext;
import org.jcnc.snow.compiler.parser.ast.ExpressionStatementNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;

/**
 * 普通表达式语句处理器。
 * <p>
 * 负责将AST中的普通表达式语句（如“a + b;”或“foo();”）
 * 编译为对应的IR层指令。常用于执行具有副作用的表达式调用或运算。
 * </p>
 */
public class ExpressionStmtHandler implements IStatementHandler {
    /**
     * 判断是否可以处理给定的语句节点。
     *
     * @param stmt AST语句节点
     * @return 若为ExpressionStatementNode类型则返回true，否则返回false
     */
    @Override
    public boolean canHandle(StatementNode stmt) {
        return stmt instanceof ExpressionStatementNode;
    }

    /**
     * 处理普通表达式语句节点，生成对应的IR指令。
     * <p>
     * 只进行表达式构建，不关心其结果值（即丢弃其求值结果），
     * 常用于过程调用、副作用语句等。
     * </p>
     *
     * @param stmt AST语句节点（已保证为ExpressionStatementNode类型）
     * @param c    语句构建上下文
     * @throws IllegalStateException 若节点结构异常
     */
    @Override
    public void handle(StatementNode stmt, StatementBuilderContext c) {
        if (stmt instanceof ExpressionStatementNode(ExpressionNode exp, NodeContext _)) {
            // 构建表达式IR，丢弃返回值
            c.expr().build(exp);
        } else {
            throw new IllegalStateException("Unexpected ExpressionStatementNode pattern");
        }
    }
}
