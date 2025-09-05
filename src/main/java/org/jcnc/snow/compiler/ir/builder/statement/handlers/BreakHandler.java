package org.jcnc.snow.compiler.ir.builder.statement.handlers;

import org.jcnc.snow.compiler.ir.builder.core.InstructionFactory;
import org.jcnc.snow.compiler.ir.builder.statement.IStatementHandler;
import org.jcnc.snow.compiler.ir.builder.statement.StatementBuilderContext;
import org.jcnc.snow.compiler.parser.ast.BreakNode;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;

/**
 * break语句处理器。
 * <p>
 * 负责将AST中的BreakNode节点编译为IR的无条件跳转指令，跳转至当前循环或块的break目标标签。
 * 自动校验上下文，确保break语句只能在合法的循环或breakable块内部使用。
 * </p>
 */
public class BreakHandler implements IStatementHandler {
    /**
     * 判断是否可以处理给定的语句节点。
     *
     * @param stmt AST语句节点
     * @return 若为BreakNode类型则返回true，否则返回false
     */
    @Override
    public boolean canHandle(StatementNode stmt) {
        return stmt instanceof BreakNode;
    }

    /**
     * 处理BreakNode节点，生成IR的break跳转指令。
     * <p>
     * 若break出现在非法位置（无break目标），则抛出异常。
     * </p>
     *
     * @param stmt AST语句节点（已保证为BreakNode类型）
     * @param c    语句构建上下文
     * @throws IllegalStateException 若break出现在循环或可break块外部
     */
    @Override
    public void handle(StatementNode stmt, StatementBuilderContext c) {
        if (c.breakTargets().isEmpty()) {
            throw new IllegalStateException("`break` appears outside of a loop");
        }
        // 跳转到当前最近的break目标
        InstructionFactory.jmp(c.ctx(), c.breakTargets().peek());
    }
}
