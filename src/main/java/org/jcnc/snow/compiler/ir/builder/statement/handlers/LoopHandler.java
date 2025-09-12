package org.jcnc.snow.compiler.ir.builder.statement.handlers;

import org.jcnc.snow.compiler.ir.builder.core.InstructionFactory;
import org.jcnc.snow.compiler.ir.builder.statement.IStatementHandler;
import org.jcnc.snow.compiler.ir.builder.statement.StatementBuilderContext;
import org.jcnc.snow.compiler.ir.builder.statement.utils.ConditionalJump;
import org.jcnc.snow.compiler.parser.ast.LoopNode;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;

/**
 * 循环语句处理器。
 * <p>
 * 负责将AST中的LoopNode节点翻译为IR层的循环控制流，
 * 支持init-条件-步进三段式循环（for/while通用）。
 * </p>
 * <ul>
 *   <li>自动管理break/continue目标，支持多层嵌套。</li>
 *   <li>生成循环头、条件判断、循环体、步进、跳转等指令。</li>
 * </ul>
 */
public class LoopHandler implements IStatementHandler {

    /**
     * 判断是否可以处理给定的语句节点。
     *
     * @param stmt AST语句节点
     * @return 若为LoopNode类型则返回true，否则返回false
     */
    @Override
    public boolean canHandle(StatementNode stmt) {
        return stmt instanceof LoopNode;
    }

    /**
     * 处理LoopNode节点，生成循环控制流的IR指令。
     * <p>
     * 流程如下：
     * <ol>
     *   <li>可选执行初始化语句</li>
     *   <li>生成循环起始标签和结束标签</li>
     *   <li>条件不成立则跳出循环</li>
     *   <li>维护break/continue目标栈（支持嵌套）</li>
     *   <li>执行循环体，支持递归嵌套</li>
     *   <li>执行步进语句（若有）</li>
     *   <li>循环尾部跳回循环头，最后落到循环结束标签</li>
     * </ol>
     * </p>
     *
     * @param stmt AST语句节点（已保证为LoopNode类型）
     * @param c    语句构建上下文
     */
    @Override
    public void handle(StatementNode stmt, StatementBuilderContext c) {
        LoopNode loop = (LoopNode) stmt;

        // 1. 处理可选的初始化语句
        if (loop.init() != null) c.build(loop.init());

        // 2. 创建循环起始和结束标签
        String lblStart = c.ctx().newLabel();
        String lblEnd = c.ctx().newLabel();

        InstructionFactory.label(c.ctx(), lblStart);

        // 3. 条件不满足则跳出循环
        ConditionalJump.emit(loop.cond(), lblEnd, c);

        // 4. 入栈break/continue目标（支持多层嵌套break/continue）
        c.breakTargets().push(lblEnd);
        String lblStep = c.ctx().newLabel();
        c.continueTargets().push(lblStep);
        try {
            // 5. 构建循环体（支持嵌套、递归）
            c.buildAll(loop.body());
        } finally {
            c.breakTargets().pop();
            c.continueTargets().pop();
        }

        // 6. 处理步进语句
        InstructionFactory.label(c.ctx(), lblStep);
        if (loop.step() != null) c.build(loop.step());

        // 7. 跳回循环起始标签，循环结束标签收尾
        InstructionFactory.jmp(c.ctx(), lblStart);
        InstructionFactory.label(c.ctx(), lblEnd);
    }
}
