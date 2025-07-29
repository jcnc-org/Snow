package org.jcnc.snow.compiler.parser.statement;

import org.jcnc.snow.compiler.lexer.token.TokenType;
import org.jcnc.snow.compiler.parser.ast.ContinueNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.parser.context.ParserContext;

/**
 * {@code ContinueStatementParser} 用于解析 continue 语句。
 * <p>
 * continue 语句语法格式简单，仅包含关键字本身，随后以换行结束。
 * 语义：跳过本次剩余循环体，继续执行 step → cond。
 * </p>
 */
public class ContinueStatementParser implements StatementParser {

    /**
     * 解析 continue 语句节点。
     * <p>
     * 期望格式为：'continue' NEWLINE
     * </p>
     *
     * @param ctx 解析上下文
     * @return ContinueNode AST 节点
     */
    @Override
    public ContinueNode parse(ParserContext ctx) {
        // 记录当前位置作为 NodeContext
        int line = ctx.getTokens().peek().getLine();
        int column = ctx.getTokens().peek().getCol();
        String file = ctx.getSourceName();

        // 消耗 'continue'
        ctx.getTokens().expect("continue");
        // 行结束
        ctx.getTokens().expectType(TokenType.NEWLINE);

        return new ContinueNode(new NodeContext(line, column, file));
    }
}
