package org.jcnc.snow.compiler.parser.expression;

import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.parser.ast.NumberLiteralNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.expression.base.PrefixParselet;

/**
 * {@code NumberLiteralParselet} 是用于解析数字字面量的前缀解析器。
 * <p>
 * 适用于处理如 {@code 42}、{@code 3.14} 等整型或浮点型常量表达式，
 * 通常出现在表达式的起始位置或子表达式内部。
 * </p>
 */
public class NumberLiteralParselet implements PrefixParselet {

    /**
     * 将当前的数字 Token 转换为 {@link NumberLiteralNode} 节点。
     *
     * @param ctx   当前语法解析上下文（此实现未使用）
     * @param token 当前的数字字面量 Token
     * @return 构建完成的数字表达式节点
     */
    @Override
    public ExpressionNode parse(ParserContext ctx, Token token) {
        return new NumberLiteralNode(token.getLexeme());
    }
}