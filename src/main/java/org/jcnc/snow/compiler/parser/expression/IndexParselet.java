package org.jcnc.snow.compiler.parser.expression;

import org.jcnc.snow.compiler.lexer.token.TokenType;
import org.jcnc.snow.compiler.parser.ast.IndexExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.expression.base.InfixParselet;

/**
 * {@code IndexParselet} 负责解析数组或集合的下标访问表达式，语法结构为：
 * <pre>
 *   primary '[' expr ']'
 * </pre>
 * 例如 {@code arr[expr]}。
 */
public class IndexParselet implements InfixParselet {

    /**
     * 解析下标访问表达式。
     *
     * @param ctx  解析上下文，包含词法流、错误信息等
     * @param left 已解析的 primary 表达式（如数组名、表达式等）
     * @return {@link IndexExpressionNode} 下标访问表达式节点
     */
    @Override
    public ExpressionNode parse(ParserContext ctx, ExpressionNode left) {
        int line = left.context().line();
        int col = left.context().column();
        String file = left.context().file();

        // 消耗左中括号 '['
        ctx.getTokens().expectType(TokenType.LBRACKET);
        // 解析索引表达式
        ExpressionNode index = new PrattExpressionParser().parse(ctx);
        // 消耗右中括号 ']'
        ctx.getTokens().expectType(TokenType.RBRACKET);
        // 构造下标访问表达式节点
        return new IndexExpressionNode(left, index, new NodeContext(line, col, file));
    }

    /**
     * 下标访问优先级与函数调用一致。
     *
     * @return {@link Precedence#CALL} 优先级
     */
    @Override
    public Precedence precedence() {
        return Precedence.CALL;
    }
}
