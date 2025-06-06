package org.jcnc.snow.compiler.lexer.scanners;

import org.jcnc.snow.compiler.lexer.core.LexerContext;
import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.lexer.token.TokenType;

/**
 * 注释扫描器：处理源代码中的注释部分，包括：
 * <ul>
 *     <li>单行注释（以 "//" 开头，直到行尾）</li>
 *     <li>多行注释（以 "/*" 开头，以 "*&#47;" 结尾）</li>
 * </ul>
 * <p>
 * 本扫描器会识别注释并生成 {@code TokenType.COMMENT} 类型的 Token，
 * 不会丢弃注释内容，而是将完整注释文本保留在 Token 中，便于后续分析（如文档提取、保留注释等场景）。
 * </p>
 */
public class CommentTokenScanner extends AbstractTokenScanner {

    /**
     * 判断是否可以处理当前位置的字符。
     * <p>当当前位置字符为 '/' 且下一个字符为 '/' 或 '*' 时，表示可能是注释的起始。</p>
     *
     * @param c   当前字符
     * @param ctx 当前词法上下文
     * @return 如果是注释的起始符，则返回 true
     */
    @Override
    public boolean canHandle(char c, LexerContext ctx) {
        return c == '/' && (ctx.peekNext() == '/' || ctx.peekNext() == '*');
    }

    /**
     * 实现注释的扫描逻辑。
     * <p>支持两种注释格式：</p>
     * <ul>
     *     <li><b>单行注释：</b> 以 "//" 开头，直到遇到换行符</li>
     *     <li><b>多行注释：</b> 以 "/*" 开头，直到遇到 "*&#47;" 结束</li>
     * </ul>
     *
     * @param ctx  词法上下文
     * @param line 当前行号（用于 Token 位置信息）
     * @param col  当前列号（用于 Token 位置信息）
     * @return 包含完整注释内容的 COMMENT 类型 Token
     */
    @Override
    protected Token scanToken(LexerContext ctx, int line, int col) {
        // 消费第一个 '/' 字符
        ctx.advance();
        StringBuilder sb = new StringBuilder("/");

        // 处理单行注释 //
        if (ctx.match('/')) {
            sb.append('/');
            while (!ctx.isAtEnd() && ctx.peek() != '\n') {
                sb.append(ctx.advance());
            }
        }
        // 处理多行注释 /* ... */
        else if (ctx.match('*')) {
            sb.append('*');
            while (!ctx.isAtEnd()) {
                char ch = ctx.advance();
                sb.append(ch);
                if (ch == '*' && ctx.peek() == '/') {
                    sb.append(ctx.advance()); // 消费 '/'
                    break;
                }
            }
        }

        return new Token(TokenType.COMMENT, sb.toString(), line, col);
    }
}
