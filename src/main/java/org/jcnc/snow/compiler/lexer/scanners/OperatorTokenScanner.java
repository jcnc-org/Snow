package org.jcnc.snow.compiler.lexer.scanners;

import org.jcnc.snow.compiler.lexer.core.LexerContext;
import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.lexer.token.TokenType;

/**
 * 运算符扫描器（OperatorTokenScanner）
 *
 * <p>负责在词法分析阶段识别由 <b>= ! &lt; &gt; | &amp; %</b> 等字符
 * 起始的单字符或双字符运算符，并生成相应 {@link Token}：</p>
 *
 * <ul>
 *   <li>赋值 / 比较：{@code =}, {@code ==}, {@code !=}</li>
 *   <li>关系运算：{@code >}, {@code >=}, {@code <}, {@code <=}</li>
 *   <li>逻辑运算：{@code &&}, {@code ||}</li>
 *   <li>取模运算：{@code %}</li>
 *   <li>逻辑非：{@code !}</li>
 * </ul>
 *
 * <p>如果无法匹配到合法组合，将返回 {@link TokenType#UNKNOWN}。</p>
 */
public class OperatorTokenScanner extends AbstractTokenScanner {

    /**
     * 判断当前字符是否可能是运算符的起始字符。
     *
     * @param c   当前字符
     * @param ctx 词法上下文
     * @return 若是关注的起始字符则返回 {@code true}
     */
    @Override
    public boolean canHandle(char c, LexerContext ctx) {
        return "=!<>|&%".indexOf(c) >= 0;
    }

    /**
     * 按最长匹配优先原则扫描并生成运算符 token。
     *
     * @param ctx  词法上下文
     * @param line 当前行号
     * @param col  当前列号
     * @return 已识别的 {@link Token}
     */
    @Override
    protected Token scanToken(LexerContext ctx, int line, int col) {
        char c = ctx.advance();
        String lexeme;
        TokenType type;

        switch (c) {
            case '=':
                if (ctx.match('=')) {
                    lexeme = "==";
                    type   = TokenType.DOUBLE_EQUALS;
                } else {
                    lexeme = "=";
                    type   = TokenType.EQUALS;
                }
                break;

            case '!':
                if (ctx.match('=')) {
                    lexeme = "!=";
                    type   = TokenType.NOT_EQUALS;
                } else {
                    lexeme = "!";
                    type   = TokenType.NOT;
                }
                break;

            case '>':
                if (ctx.match('=')) {
                    lexeme = ">=";
                    type   = TokenType.GREATER_EQUAL;
                } else {
                    lexeme = ">";
                    type   = TokenType.GREATER_THAN;
                }
                break;

            case '<':
                if (ctx.match('=')) {
                    lexeme = "<=";
                    type   = TokenType.LESS_EQUAL;
                } else {
                    lexeme = "<";
                    type   = TokenType.LESS_THAN;
                }
                break;

            case '%':
                lexeme = "%";
                type   = TokenType.MODULO;
                break;

            case '&':
                if (ctx.match('&')) {
                    lexeme = "&&";
                    type   = TokenType.AND;
                } else {
                    lexeme = "&";
                    type   = TokenType.UNKNOWN;
                }
                break;

            case '|':
                if (ctx.match('|')) {
                    lexeme = "||";
                    type   = TokenType.OR;
                } else {
                    lexeme = "|";
                    type   = TokenType.UNKNOWN;
                }
                break;

            default:
                lexeme = String.valueOf(c);
                type   = TokenType.UNKNOWN;
        }

        return new Token(type, lexeme, line, col);
    }
}
