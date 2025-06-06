package org.jcnc.snow.compiler.parser.expression;

import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.parser.ast.IdentifierNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.expression.base.PrefixParselet;

/**
 * {@code IdentifierParselet} 是用于解析标识符表达式的前缀解析器。
 * <p>
 * 适用于解析如 {@code x}、{@code count}、{@code isValid} 等变量名或函数名，
 * 通常出现在表达式的开头部分，是 AST 中的基础表达式节点之一。
 * </p>
 */
public class IdentifierParselet implements PrefixParselet {

    /**
     * 解析标识符表达式。
     *
     * @param ctx   当前语法解析上下文（本实现未使用）
     * @param token 当前标识符 Token
     * @return 构建的 {@link IdentifierNode} 表达式节点
     */
    @Override
    public ExpressionNode parse(ParserContext ctx, Token token) {
        return new IdentifierNode(token.getLexeme());
    }
}