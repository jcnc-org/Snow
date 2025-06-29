package org.jcnc.snow.compiler.lexer.scanners;

import org.jcnc.snow.compiler.lexer.core.LexerContext;
import org.jcnc.snow.compiler.lexer.core.LexicalException;
import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.lexer.token.TokenType;

/**
 * 数字扫描器：识别整数、小数以及带有 <strong>类型后缀</strong> 的数字字面量。<br>
 * <p>
 * 支持格式示例：
 * <ul>
 *     <li>整数：<code>123</code>、<code>0</code>、<code>45678</code></li>
 *     <li>小数：<code>3.14</code>、<code>0.5</code>、<code>12.0</code></li>
 *     <li>带后缀：<code>2.0f</code>、<code>42L</code>、<code>7s</code>、<code>255B</code></li>
 * </ul>
 * </p>
 * <p>
 * 单字符类型后缀：
 * <pre>
 * b | s | l | f | d   // byte, short, long, float, double
 * B | S | L | F | D   // 同上（大小写均可）
 * </pre>
 * </p>
 * <p>
 * 规则约束：<br>
 * 若数字主体之后出现以下情况，将在词法阶段抛出 {@link LexicalException}：
 * <ul>
 *     <li>空白 + 字母（如 <code>3&nbsp;L</code>）</li>
 *     <li>未知字母紧邻（如 <code>3E</code>）</li>
 *     <li><code>'/'</code> 紧邻（如 <code>3/</code>、<code>3/*</code>）</li>
 * </ul>
 * 以避免编译器陷入死循环。
 * </p>
 */
public class NumberTokenScanner extends AbstractTokenScanner {

    /** 合法类型后缀字符集合 */
    private static final String SUFFIX_CHARS = "bslfdBSLFD";

    @Override
    public boolean canHandle(char c, LexerContext ctx) {
        return Character.isDigit(c);
    }

    @Override
    protected Token scanToken(LexerContext ctx, int line, int col) {
        StringBuilder literal = new StringBuilder();
        boolean hasDot = false; // 是否已遇到小数点

        /* 1. 读取数字主体（整数 / 小数） */
        while (!ctx.isAtEnd()) {
            char c = ctx.peek();
            if (c == '.' && !hasDot) {
                hasDot = true;
                literal.append(ctx.advance());
            } else if (Character.isDigit(c)) {
                literal.append(ctx.advance());
            } else {
                break;
            }
        }

        /* 2. 处理后缀或非法跟随字符 */
        if (!ctx.isAtEnd()) {
            char next = ctx.peek();

            /* 2-A: 合法类型后缀，直接吸收 */
            if (SUFFIX_CHARS.indexOf(next) >= 0) {
                literal.append(ctx.advance());
            }
            /* 2-B: 未知字母紧邻 → 抛异常 */
            else if (Character.isLetter(next)) {
                throw new LexicalException(
                        "Unknown numeric suffix '" + next + "'",
                        line, col
                );
            }
            /* 2-C: 数字后空白（非换行）→ 若空白后跟字母，抛异常 */
            else if (Character.isWhitespace(next) && next != '\n') {
                int off = 1;
                char look;
                do {
                    look = ctx.peekAhead(off);
                    if (look == '\n' || look == '\0') break;
                    if (!Character.isWhitespace(look)) break;
                    off++;
                } while (true);

                if (Character.isLetter(look)) {
                    throw new LexicalException(
                            "Whitespace between numeric literal and an alphabetic character is not allowed",
                            line, col
                    );
                }
            }
            /* 2-D: 紧邻字符为 '/' → 抛异常以避免死循环 */
            else if (next == '/') {
                throw new LexicalException(
                        "Unexpected '/' after numeric literal",
                        line, col
                );
            }
            /* 其余字符（运算符、分隔符等）留给后续扫描器处理 */
        }

        /* 3. 返回 NUMBER_LITERAL Token */
        return new Token(TokenType.NUMBER_LITERAL, literal.toString(), line, col);
    }
}
