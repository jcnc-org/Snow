package org.jcnc.snow.compiler.parser.expression;

import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.expression.base.PrefixParselet;

/**
 * {@code GroupingParselet} 解析圆括号括起的子表达式。
 * <p>
 * 用于处理形如 {@code (a + b)} 的表达式结构，
 * 通常用于提升括号内表达式的优先级，以控制运算顺序。
 * </p>
 */
public class GroupingParselet implements PrefixParselet {

    /**
     * 解析括号表达式。
     * <p>
     * 该方法假定当前 token 为左括号 "("，将解析其内部表达式，
     * 并断言后续 token 为右括号 ")"。
     * </p>
     *
     * @param ctx   当前解析上下文
     * @param token 当前起始 token，应为 "("
     * @return 被括号包裹的子表达式节点
     */
    @Override
    public ExpressionNode parse(ParserContext ctx, Token token) {
        ExpressionNode expr = new PrattExpressionParser().parse(ctx);
        ctx.getTokens().expect(")");
        return expr;
    }
}
