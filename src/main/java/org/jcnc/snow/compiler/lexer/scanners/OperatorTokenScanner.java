package org.jcnc.snow.compiler.lexer.scanners;

import org.jcnc.snow.compiler.lexer.core.LexerContext;
import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.lexer.token.TokenType;

/**
 * 运算符扫描器：识别逻辑与比较运算符，包括单字符和双字符组合。
 * <p>
 * 支持的运算符包括：
 * <ul>
 *     <li>赋值与比较：=、==、!=</li>
 *     <li>关系运算符：&gt;、&gt;=、&lt;、&lt;=</li>
 *     <li>逻辑运算符：&&、||</li>
 * </ul>
 * <p>
 * 不符合上述组合的字符会返回 {@code UNKNOWN} 类型的 Token。
 */
public class OperatorTokenScanner extends AbstractTokenScanner {

    /**
     * 判断是否可以处理当前位置的字符。
     * <p>运算符扫描器关注的起始字符包括：=、!、&lt;、&gt;、|、&amp;</p>
     *
     * @param c   当前字符
     * @param ctx 当前词法上下文
     * @return 如果是潜在的运算符起始字符，则返回 true
     */
    @Override
    public boolean canHandle(char c, LexerContext ctx) {
        return "=!<>|&%".indexOf(c) >= 0;
    }

    /**
     * 扫描并识别运算符 Token。
     * <p>支持组合运算符判断，如 ==、!=、&gt;= 等，
     * 若无法匹配组合形式则退回单字符形式。</p>
     *
     * @param ctx 词法上下文
     * @param line 当前行号
     * @param col 当前列号
     * @return 对应的运算符 Token，无法识别的运算符返回 {@code UNKNOWN}
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
                    type = TokenType.DOUBLE_EQUALS;
                } else {
                    lexeme = "=";
                    type = TokenType.EQUALS;
                }
                break;
            case '!':
                if (ctx.match('=')) {
                    lexeme = "!=";
                    type = TokenType.NOT_EQUALS;
                } else {
                    lexeme = "!";
                    type = TokenType.NOT;
                }
                break;
            case '>':
                if (ctx.match('=')) {
                    lexeme = ">=";
                    type = TokenType.GREATER_EQUAL;
                } else {
                    lexeme = ">";
                    type = TokenType.GREATER_THAN;
                }
                break;
            case '<':
                if (ctx.match('=')) {
                    lexeme = "<=";
                    type = TokenType.LESS_EQUAL;
                } else {
                    lexeme = "<";
                    type = TokenType.LESS_THAN;
                }
                break;
            case '%':
                lexeme = "%";
                type = TokenType.MODULO;
                break;
            case '&':
                if (ctx.match('&')) {
                    lexeme = "&&";
                    type = TokenType.AND;
                } else {
                    lexeme = "&";
                    type = TokenType.UNKNOWN;
                }
                break;
            case '|':
                if (ctx.match('|')) {
                    lexeme = "||";
                    type = TokenType.OR;
                } else {
                    lexeme = "|";
                    type = TokenType.UNKNOWN;
                }
                break;
            default:
                lexeme = String.valueOf(c);
                type = TokenType.UNKNOWN;
        }

        return new Token(type, lexeme, line, col);
    }
}
