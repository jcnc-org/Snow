package org.jcnc.snow.compiler.lexer.scanners;

import org.jcnc.snow.compiler.lexer.core.LexerContext;
import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.lexer.token.TokenType;

/**
 * 字符串扫描器：处理双引号包裹的字符串字面量，支持基本的转义字符。
 * <p>
 * 支持格式示例：
 * <ul>
 *     <li>"hello"</li>
 *     <li>"line\\nbreak"</li>
 *     <li>"escaped \\\" quote"</li>
 * </ul>
 * <p>
 * 扫描器会保留原始字符串的形式（包含双引号和转义符），
 * 并生成 {@code STRING_LITERAL} 类型的 Token。
 */
public class StringTokenScanner extends AbstractTokenScanner {

    /**
     * 判断是否可以处理当前位置的字符。
     * <p>当字符为双引号（"）时，认为是字符串字面量的开始。</p>
     *
     * @param c   当前字符
     * @param ctx 当前词法上下文
     * @return 如果为字符串起始符，则返回 true
     */
    @Override
    public boolean canHandle(char c, LexerContext ctx) {
        return c == '"';
    }

    /**
     * 执行字符串的扫描逻辑。
     * <p>从当前位置开始，读取直到匹配结束的双引号。
     * 支持转义字符（如 \"、\\n 等），不会中断字符串扫描。</p>
     *
     * @param ctx  词法上下文
     * @param line 当前行号
     * @param col  当前列号
     * @return 字符串字面量类型的 Token
     */
    @Override
    protected Token scanToken(LexerContext ctx, int line, int col) {
        StringBuilder sb = new StringBuilder();
        sb.append(ctx.advance()); // 起始双引号

        while (!ctx.isAtEnd()) {
            char c = ctx.advance();
            sb.append(c);

            if (c == '\\') {
                sb.append(ctx.advance()); // 添加转义字符后的实际字符
            } else if (c == '"') {
                break;
            }
        }

        return new Token(TokenType.STRING_LITERAL, sb.toString(), line, col);
    }
}
