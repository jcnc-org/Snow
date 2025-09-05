package org.jcnc.snow.compiler.ir.builder.statement.handlers;

import org.jcnc.snow.compiler.ir.builder.core.InstructionFactory;
import org.jcnc.snow.compiler.ir.builder.statement.IStatementHandler;
import org.jcnc.snow.compiler.ir.builder.statement.StatementBuilderContext;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.compiler.parser.ast.ReturnNode;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;

/**
 * return语句处理器。
 * <p>
 * 负责将AST中的ReturnNode节点翻译为IR的返回指令，
 * 支持带返回值和不带返回值两种情况。
 * </p>
 */
public class ReturnHandler implements IStatementHandler {

    /**
     * 判断是否可以处理给定的语句节点。
     *
     * @param stmt AST语句节点
     * @return 若为ReturnNode类型则返回true，否则返回false
     */
    @Override
    public boolean canHandle(StatementNode stmt) {
        return stmt instanceof ReturnNode;
    }

    /**
     * 处理ReturnNode节点，生成IR返回指令。
     * <ul>
     *   <li>若有返回表达式，先构建表达式并生成带值的ret指令。</li>
     *   <li>若无返回表达式，直接生成void ret指令。</li>
     * </ul>
     *
     * @param stmt 需处理的AST语句节点（已保证为ReturnNode）
     * @param c    语句构建上下文（含IR环境、表达式构建器等）
     */
    @Override
    public void handle(StatementNode stmt, StatementBuilderContext c) {
        ReturnNode ret = (ReturnNode) stmt;
        if (ret.getExpression().isPresent()) {
            // 处理带返回值情况
            IRVirtualRegister r = c.expr().build(ret.getExpression().get());
            InstructionFactory.ret(c.ctx(), r);
        } else {
            // 处理无返回值情况
            InstructionFactory.retVoid(c.ctx());
        }
    }
}
