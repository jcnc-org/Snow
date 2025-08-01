package org.jcnc.snow.compiler.parser.expression;

import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.lexer.token.TokenType;
import org.jcnc.snow.compiler.parser.ast.ArrayLiteralNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.context.TokenStream;
import org.jcnc.snow.compiler.parser.expression.base.PrefixParselet;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code ArrayLiteralParselet} 用于解析数组字面量表达式。
 * <p>
 * 支持语法形如 {@code [a, b, c]} 或 {@code [ [1,2], [3,4] ]}，
 * 允许元素及逗号前后出现换行符，便于多行书写。
 * </p>
 * <p>
 * 语法结构为：<br>
 * <pre>[ (element (',' element)*)? ]</pre>
 * </p>
 */
public class ArrayLiteralParselet implements PrefixParselet {

    /**
     * 解析数组字面量表达式。
     *
     * @param ctx   解析上下文（包含词法流、源文件信息等）
     * @param token 已消费的左中括号（LBRACKET），用于定位节点源信息
     * @return 解析得到的 {@link ArrayLiteralNode} 节点
     */
    @Override
    public ExpressionNode parse(ParserContext ctx, Token token) {
        // token 为已消费的 LBRACKET，使用其位置生成 NodeContext
        int line = token.getLine();
        int col  = token.getCol();
        String file = ctx.getSourceName();

        TokenStream ts = ctx.getTokens();
        skipNewlines(ts);

        List<ExpressionNode> elements = new ArrayList<>();

        // 空数组: 直接遇到 RBRACKET
        if (ts.peek().getType() != TokenType.RBRACKET) {
            while (true) {
                // 解析一个元素
                ExpressionNode elem = new PrattExpressionParser().parse(ctx);
                elements.add(elem);

                skipNewlines(ts);
                // 逗号继续，右中括号结束
                if (ts.peek().getType() == TokenType.COMMA) {
                    ts.next();
                    skipNewlines(ts);
                    continue;
                }
                break;
            }
        }

        // 期望并消费右中括号
        ts.expectType(TokenType.RBRACKET);
        return new ArrayLiteralNode(elements, new NodeContext(line, col, file));
    }

    /**
     * 跳过词法流中连续的换行符，允许数组元素跨多行书写。
     *
     * @param ts 词法流
     */
    private static void skipNewlines(TokenStream ts) {
        while (ts.peek().getType() == TokenType.NEWLINE) {
            ts.next();
        }
    }
}
