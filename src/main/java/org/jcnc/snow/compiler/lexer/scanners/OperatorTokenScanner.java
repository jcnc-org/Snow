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
        String lexeme = String.valueOf(c);
        TokenType type = TokenType.UNKNOWN;

        // 当前状态
        State currentState = State.OPERATOR;

        switch (c) {
            case '=':
                if (ctx.match('=')) {
                    lexeme = "==";
                    type = TokenType.DOUBLE_EQUALS;
                } else {
                    type = TokenType.EQUALS;
                }
                break;

            case '!':
                if (ctx.match('=')) {
                    lexeme = "!=";
                    type = TokenType.NOT_EQUALS;
                } else {
                    type = TokenType.NOT;
                }
                break;

            case '>':
                if (ctx.match('=')) {
                    lexeme = ">=";
                    type = TokenType.GREATER_EQUAL;
                } else {
                    type = TokenType.GREATER_THAN;
                }
                break;

            case '<':
                if (ctx.match('=')) {
                    lexeme = "<=";
                    type = TokenType.LESS_EQUAL;
                } else {
                    type = TokenType.LESS_THAN;
                }
                break;

            case '%':
                type = TokenType.MODULO;
                break;

            case '&':
                if (ctx.match('&')) {
                    lexeme = "&&";
                    type = TokenType.AND;
                }
                break;

            case '|':
                if (ctx.match('|')) {
                    lexeme = "||";
                    type = TokenType.OR;
                }
                break;

            default:
                currentState = State.UNKNOWN;
                break;
        }

        // 执行完扫描后，重置状态为初始状态
        if (currentState != State.UNKNOWN) {
            currentState = State.START;
        }

        return new Token(type, lexeme, line, col);
    }

    // 定义状态枚举
    private enum State {
        START,        // 初始状态
        OPERATOR,     // 当前字符是运算符的一部分
        UNKNOWN       // 无法识别的状态
    }
}
