package org.jcnc.snow.compiler.ir.builder.statement.handlers;

import org.jcnc.snow.compiler.ir.builder.core.InstructionFactory;
import org.jcnc.snow.compiler.ir.builder.statement.IStatementHandler;
import org.jcnc.snow.compiler.ir.builder.statement.utils.ConditionalJump;
import org.jcnc.snow.compiler.ir.builder.statement.StatementBuilderContext;
import org.jcnc.snow.compiler.parser.ast.IfNode;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;

/**
 * if语句处理器。
 * <p>
 * 负责将AST中的IfNode节点编译为IR的条件跳转与分支控制流，
 * 支持then分支与可选的else分支，自动生成条件跳转、分支标签和收敛标签。
 * </p>
 */
public class IfHandler implements IStatementHandler {
    /**
     * 判断是否可以处理给定的语句节点。
     *
     * @param stmt AST语句节点
     * @return 若为IfNode类型则返回true，否则返回false
     */
    @Override
    public boolean canHandle(StatementNode stmt) {
        return stmt instanceof IfNode;
    }

    /**
     * 处理IfNode节点，生成IR的条件分支控制流。
     * <p>
     * 实现思路：
     * <ol>
     *   <li>对条件表达式生成条件跳转，不成立时跳转到else分支。</li>
     *   <li>构建then分支指令，最后无条件跳转到分支合流（end）标签。</li>
     *   <li>在else标签下构建else分支（若有）。</li>
     *   <li>最后生成分支合流标签。</li>
     * </ol>
     * </p>
     *
     * @param stmt AST语句节点（已保证为IfNode类型）
     * @param c    语句构建上下文
     */
    @Override
    public void handle(StatementNode stmt, StatementBuilderContext c) {
        IfNode ifNode = (IfNode) stmt;

        // 1. 分配else与end标签
        String lblElse = c.ctx().newLabel();
        String lblEnd = c.ctx().newLabel();

        // 2. 条件不成立跳到else分支
        ConditionalJump.emit(ifNode.condition(), lblElse, c);

        // 3. 构建then分支
        c.buildAll(ifNode.thenBranch());
        InstructionFactory.jmp(c.ctx(), lblEnd);

        // 4. 构建else分支（如有）
        InstructionFactory.label(c.ctx(), lblElse);
        if (ifNode.elseBranch() != null) c.buildAll(ifNode.elseBranch());

        // 5. 分支合流标签
        InstructionFactory.label(c.ctx(), lblEnd);
    }
}
