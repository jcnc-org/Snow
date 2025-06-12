package org.jcnc.snow.compiler.parser.expression;

import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.parser.ast.UnaryExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.expression.base.PrefixParselet;

/** 前缀一元运算解析器（支持 - 和 !） */
public class UnaryOperatorParselet implements PrefixParselet {

    @Override
    public ExpressionNode parse(ParserContext ctx, Token token) {
        // 递归解析右侧，使用自身优先级
        ExpressionNode right =
                new PrattExpressionParser().parseExpression(ctx, Precedence.UNARY);
        return new UnaryExpressionNode(token.getLexeme(), right);
    }
}
