package org.jcnc.snow.compiler.ir.builder.statement.handlers;

import org.jcnc.snow.compiler.ir.builder.core.InstructionFactory;
import org.jcnc.snow.compiler.ir.builder.statement.IStatementHandler;
import org.jcnc.snow.compiler.ir.builder.statement.StatementBuilderContext;
import org.jcnc.snow.compiler.parser.ast.ContinueNode;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;

/**
 * continue语句处理器。
 * <p>
 * 负责将AST中的ContinueNode节点编译为IR的无条件跳转指令，跳转至当前循环的步进/continue目标。
 * 自动校验上下文，保证continue只在合法的循环体中使用。
 * </p>
 */
public class ContinueHandler implements IStatementHandler {
    /**
     * 判断是否可以处理给定的语句节点。
     *
     * @param stmt AST语句节点
     * @return 若为ContinueNode类型则返回true，否则返回false
     */
    @Override
    public boolean canHandle(StatementNode stmt) {
        return stmt instanceof ContinueNode;
    }

    /**
     * 处理ContinueNode节点，生成IR的continue跳转指令。
     * <p>
     * 若不在合法的循环体内（continue目标栈为空），将抛出异常。
     * </p>
     *
     * @param stmt AST语句节点（已保证为ContinueNode类型）
     * @param c    语句构建上下文
     * @throws IllegalStateException 若continue出现在循环外部
     */
    @Override
    public void handle(StatementNode stmt, StatementBuilderContext c) {
        if (c.continueTargets().isEmpty()) {
            throw new IllegalStateException("`continue` appears outside of a loop");
        }
        // 跳转到当前最近的continue目标
        InstructionFactory.jmp(c.ctx(), c.continueTargets().peek());
    }
}
