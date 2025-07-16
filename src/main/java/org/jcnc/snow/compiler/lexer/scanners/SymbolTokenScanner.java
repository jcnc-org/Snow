package org.jcnc.snow.compiler.lexer.scanners;

import org.jcnc.snow.compiler.lexer.core.LexerContext;
import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.lexer.token.TokenType;

/**
 * 符号扫描器: 识别常见的单字符符号，如冒号、逗号、括号和算术符号。
 * <p>
 * 支持的符号包括: 
 * <ul>
 *     <li>标点符号: : , .</li>
 *     <li>括号: ( )</li>
 *     <li>算术运算符: + - *</li>
 * </ul>
 * <p>
 * 生成的 Token 类型根据字符分别对应 {@link TokenType} 枚举中的定义。
 */
public class SymbolTokenScanner extends AbstractTokenScanner {

    /**
     * 判断是否可以处理当前位置的字符。
     * <p>本扫描器处理的符号包括 : , ( ) . + - *</p>
     *
     * @param c   当前字符
     * @param ctx 当前词法上下文
     * @return 如果是支持的符号字符，则返回 true
     */
    @Override
    public boolean canHandle(char c, LexerContext ctx) {
        return ":,().+-*/".indexOf(c) >= 0;
    }

    /**
     * 执行符号的扫描逻辑。
     * <p>根据字符匹配对应的 {@code TokenType} 类型，构造并返回 Token。</p>
     *
     * @param ctx  词法上下文
     * @param line 当前行号
     * @param col  当前列号
     * @return 表示符号的 Token
     */
    @Override
    protected Token scanToken(LexerContext ctx, int line, int col) {
        char c = ctx.advance();
        TokenType type = switch (c) {
            case ':' -> TokenType.COLON;
            case ',' -> TokenType.COMMA;
            case '.' -> TokenType.DOT;
            case '+' -> TokenType.PLUS;
            case '-' -> TokenType.MINUS;
            case '*' -> TokenType.MULTIPLY;
            case '/' -> TokenType.DIVIDE;
            case '(' -> TokenType.LPAREN;
            case ')' -> TokenType.RPAREN;
            default -> TokenType.UNKNOWN;
        };
        return new Token(type, String.valueOf(c), line, col);
    }
}
