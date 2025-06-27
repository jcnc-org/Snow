package org.jcnc.snow.compiler.parser.expression;

import org.jcnc.snow.compiler.lexer.token.TokenType;
import org.jcnc.snow.compiler.parser.ast.MemberExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.context.TokenStream;
import org.jcnc.snow.compiler.parser.expression.base.InfixParselet;

/**
 * {@code MemberParselet} 是用于解析成员访问表达式的中缀解析器。
 * <p>
 * 解析形如 {@code object.property} 的语法结构，常用于访问对象字段或方法，
 * 是一种紧绑定操作，优先级通常与函数调用相同。
 * </p>
 */
public class MemberParselet implements InfixParselet {

    /**
     * 解析成员访问表达式。
     *
     * @param ctx  当前解析上下文
     * @param left 已解析的对象表达式（左侧）
     * @return {@link MemberExpressionNode} 表示成员访问的表达式节点
     */
    @Override
    public ExpressionNode parse(ParserContext ctx, ExpressionNode left) {
        TokenStream ts = ctx.getTokens();
        ts.expect("."); // 消费点号

        // 获取当前 token 的行号、列号和文件名
        int line = ctx.getTokens().peek().getLine();
        int column = ctx.getTokens().peek().getCol();
        String file = ctx.getSourceName();

        String member = ts.expectType(TokenType.IDENTIFIER).getLexeme();
        return new MemberExpressionNode(left, member, line, column, file);
    }

    /**
     * 获取成员访问操作的优先级。
     *
     * @return 表达式优先级 {@link Precedence#CALL}
     */
    @Override
    public Precedence precedence() {
        return Precedence.CALL;
    }
}