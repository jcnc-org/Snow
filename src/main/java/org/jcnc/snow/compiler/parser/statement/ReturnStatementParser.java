package org.jcnc.snow.compiler.parser.statement;

import org.jcnc.snow.compiler.lexer.token.TokenType;
import org.jcnc.snow.compiler.parser.ast.ReturnNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.expression.PrattExpressionParser;

/**
 * {@code ReturnStatementParser} 负责解析 return 语句，是语句级解析器的一部分。
 * <p>
 * 支持以下两种 return 语句形式：
 * <pre>{@code
 * return             // 无返回值
 * return expression  // 带返回值
 * }</pre>
 * 所有 return 语句都必须以换行符（{@code NEWLINE}）结束，返回值表达式（若存在）由 {@link PrattExpressionParser} 负责解析。
 * 若语法结构不满足要求，将在解析过程中抛出异常。
 */
public class ReturnStatementParser implements StatementParser {

    /**
     * 解析一条 return 语句，并返回对应的 {@link ReturnNode} 抽象语法树节点。
     * <p>
     * 解析逻辑如下：
     * <ol>
     *     <li>匹配起始关键字 {@code return}。</li>
     *     <li>判断其后是否为 {@code NEWLINE}，若否则表示存在返回值表达式。</li>
     *     <li>使用 {@link PrattExpressionParser} 解析返回值表达式（若存在）。</li>
     *     <li>最后匹配换行符，标志语句结束。</li>
     * </ol>
     *
     * @param ctx 当前解析上下文，包含词法流与语法状态。
     * @return 构造完成的 {@link ReturnNode}，表示 return 语句的语法树节点。
     */
    @Override
    public ReturnNode parse(ParserContext ctx) {
        // 消耗 "return" 关键字
        ctx.getTokens().expect("return");

        ExpressionNode expr = null;

        // 如果下一 token 不是换行符，说明存在返回值表达式
        if (ctx.getTokens().peek().getType() != TokenType.NEWLINE) {
            expr = new PrattExpressionParser().parse(ctx);
        }

        // return 语句必须以换行符结束
        ctx.getTokens().expectType(TokenType.NEWLINE);

        // 构建并返回 ReturnNode（可能为空表达式）
        return new ReturnNode(expr);
    }
}
