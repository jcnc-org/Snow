package org.jcnc.snow.compiler.lexer.scanners;

import org.jcnc.snow.compiler.lexer.core.LexerContext;
import org.jcnc.snow.compiler.lexer.core.LexicalException;
import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.lexer.token.TokenType;

/**
 * {@code CommentTokenScanner} —— 注释解析器，基于有限状态机（FSM）。
 *
 * <p>负责将源码中的两种注释形式切分为 {@link TokenType#COMMENT COMMENT} token：</p>
 * <ol>
 *   <li>单行注释：以 {@code //} 开头，直至行尾或文件末尾。</li>
 * 多行注释：以 {@code /*} 开头，以 <code>*&#47;</code> 结束，可跨多行。
 * </ol>
 *
 * <p>本扫描器遵循“发现即捕获”原则：
 * 注释文本被完整保留在 Token 中，供后续的文档提取、源映射等分析使用。</p>
 *
 * <p>错误处理策略</p>
 * <ul>
 *   <li>未终止的多行注释：若文件结束时仍未遇到 <code>*&#47;</code>，抛出 {@link LexicalException}。</li>
 * </ul>
 */
public class CommentTokenScanner extends AbstractTokenScanner {

    /**
     * 仅当当前字符为 {@code '/'} 且下一个字符为 {@code '/'} 或 {@code '*'} 时，由本扫描器处理。
     */
    @Override
    public boolean canHandle(char c, LexerContext ctx) {
        return c == '/' && (ctx.peekNext() == '/' || ctx.peekNext() == '*');
    }

    /**
     * 执行注释扫描，生成 {@code COMMENT} Token。
     *
     * @param ctx  词法上下文
     * @param line 起始行号（1 基）
     * @param col  起始列号（1 基）
     * @return 包含完整注释文本的 Token
     * @throws LexicalException 若遇到未终止的多行注释
     */
    @Override
    protected Token scanToken(LexerContext ctx, int line, int col) {
        StringBuilder literal = new StringBuilder();

        /*
         * 1. 读取注释起始符
         *    - 已由 canHandle 保证当前位置一定是 '/'
         */
        literal.append(ctx.advance()); // 消费首个 '/'

        // -------- 单行注释 (//) --------
        if (ctx.match('/')) {
            literal.append('/');
            while (!ctx.isAtEnd() && ctx.peek() != '\n') {
                literal.append(ctx.advance());
            }
            // 行尾或文件尾时退出，换行符留给上层扫描器处理。
        }
        // -------- 多行注释 (/* ... */) --------
        else if (ctx.match('*')) {
            literal.append('*');
            boolean terminated = false;
            while (!ctx.isAtEnd()) {
                char ch = ctx.advance();
                literal.append(ch);
                if (ch == '*' && ctx.peek() == '/') {
                    literal.append(ctx.advance()); // 追加 '/'
                    terminated = true;
                    break;
                }
            }
            if (!terminated) {
                // 文件结束仍未闭合，抛 LexicalException
                throw new LexicalException("未终止的多行注释", line, col);
            }
        }

        /*
         * 2. 生成并返回 Token
         */
        return new Token(TokenType.COMMENT, literal.toString(), line, col);
    }
}
