package org.jcnc.snow.compiler.parser.statement;

import org.jcnc.snow.compiler.lexer.token.TokenType;
import org.jcnc.snow.compiler.parser.ast.BreakNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.parser.context.ParserContext;

/**
 * {@code BreakStatementParser} 用于解析 break 语句。
 * <p>
 * break 语句的语法仅包含关键字本身，随后以换行结束。
 * 语义：立即终止当前（最内层）循环。
 * </p>
 */
public class BreakStatementParser implements StatementParser {

    /**
     * 解析 break 语句节点。
     * <p>
     * 期望格式为：'break' NEWLINE
     * </p>
     *
     * @param ctx 解析上下文
     * @return BreakNode AST 节点
     */
    @Override
    public BreakNode parse(ParserContext ctx) {
        // 记录当前位置作为 NodeContext
        int line = ctx.getTokens().peek().getLine();
        int column = ctx.getTokens().peek().getCol();
        String file = ctx.getSourceName();

        // 消耗 'break'
        ctx.getTokens().expect("break");
        // 行结束
        ctx.getTokens().expectType(TokenType.NEWLINE);

        return new BreakNode(new NodeContext(line, column, file));
    }
}
