package org.jcnc.snow.compiler.parser.expression;

import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.StringLiteralNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.expression.base.PrefixParselet;

/**
 * {@code StringLiteralParselet} 是用于解析字符串字面量的前缀解析器。
 * <p>
 * 该解析器会将原始字符串 Token（带引号）转换为 {@link StringLiteralNode}，
 * 并自动去除词素前后的双引号。
 * 例如，输入 Token 为 {@code "\"hello\""}，将被解析为 {@code StringLiteralNode("hello")}。
 * </p>
 */
public class StringLiteralParselet implements PrefixParselet {

    /**
     * 解析字符串字面量 Token。
     *
     * @param ctx   当前语法解析上下文（未使用）
     * @param token 当前字符串 Token（包含引号）
     * @return {@link StringLiteralNode} 表达式节点
     */
    @Override
    public ExpressionNode parse(ParserContext ctx, Token token) {
        String raw = token.getRaw();
        String content = raw.substring(1, raw.length() - 1);
        return new StringLiteralNode(content, new NodeContext(token.getLine(), token.getCol(), ctx.getSourceName()));
    }
}