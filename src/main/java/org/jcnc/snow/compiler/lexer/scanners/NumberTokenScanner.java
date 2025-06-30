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

    /** 合法类型后缀字符集合（单字符，大小写均可） */
    private static final String SUFFIX_CHARS = "bslfdBSLFD";

    @Override
    public boolean canHandle(char c, LexerContext ctx) {
        // 仅当遇到数字时，本扫描器才处理
        return Character.isDigit(c);
    }

    @Override
    protected Token scanToken(LexerContext ctx, int line, int col) {
        StringBuilder literal = new StringBuilder();
        boolean hasDot = false; // 标记是否已出现过小数点

        /* 1. 读取数字主体部分（包括整数、小数） */
        while (!ctx.isAtEnd()) {
            char c = ctx.peek();
            if (c == '.' && !hasDot) {
                // 遇到第一个小数点
                hasDot = true;
                literal.append(ctx.advance());
            } else if (Character.isDigit(c)) {
                // 吸收数字字符
                literal.append(ctx.advance());
            } else {
                // 非数字/非小数点，终止主体读取
                break;
            }
        }

        /* 2. 检查数字字面量后的字符，决定是否继续吸收或抛出异常 */
        if (!ctx.isAtEnd()) {
            char next = ctx.peek();

            /* 2-A: 合法类型后缀，直接吸收（如 42L、3.0F） */
            if (SUFFIX_CHARS.indexOf(next) >= 0) {
                literal.append(ctx.advance());
            }
            /* 2-B: 若紧跟未知字母（如 42X），抛出词法异常 */
            else if (Character.isLetter(next)) {
                throw new LexicalException(
                        "未知的数字类型后缀 '" + next + "'",
                        line, col
                );
            }
            /* 2-C: 若数字后有空白，且空白后紧跟字母（如 3 L），也为非法 */
            else if (Character.isWhitespace(next) && next != '\n') {
                int off = 1;
                char look;
                // 跳过所有空白字符，找到第一个非空白字符
                do {
                    look = ctx.peekAhead(off);
                    if (look == '\n' || look == '\0') break;
                    if (!Character.isWhitespace(look)) break;
                    off++;
                } while (true);

                if (Character.isLetter(look)) {
                    // 抛出：数字字面量与位宽符号之间不允许有空白符
                    throw new LexicalException(
                            "数字字面量与位宽符号之间不允许有空白符",
                            line, col
                    );
                }
            }
            /* 2-D: 若紧跟 '/'，抛出异常防止死循环 */
            else if (next == '/') {
                throw new LexicalException(
                        "数字字面量后不允许直接出现 '/'",
                        line, col
                );
            }
            // 其余情况（如分号、括号、运算符），交由其他扫描器处理
        }

        /* 3. 返回 NUMBER_LITERAL Token */
        return new Token(TokenType.NUMBER_LITERAL, literal.toString(), line, col);
    }
}
