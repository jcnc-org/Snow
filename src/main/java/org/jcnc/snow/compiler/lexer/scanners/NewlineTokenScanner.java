package org.jcnc.snow.compiler.lexer.scanners;

import org.jcnc.snow.compiler.lexer.core.LexerContext;
import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.lexer.token.TokenType;

/**
 * 换行符扫描器: 将源代码中的换行符（\n）识别为 {@code NEWLINE} 类型的 Token。
 * <p>
 * 用于记录行的分界，辅助语法分析阶段进行行敏感的判断或保持结构清晰。
 */
public class NewlineTokenScanner extends AbstractTokenScanner {

    // 当前状态
    private State currentState = State.INITIAL;

    /**
     * 判断是否可以处理当前位置的字符。
     * <p>当字符为换行符（\n）时返回 true。</p>
     *
     * @param c   当前字符
     * @param ctx 当前词法上下文
     * @return 如果为换行符，则返回 true
     */
    @Override
    public boolean canHandle(char c, LexerContext ctx) {
        // 只有当处于 INITIAL 状态，并且遇到换行符时，才可以处理
        return currentState == State.INITIAL && (c == '\n' || c == '\r');
    }

    /**
     * 执行换行符的扫描逻辑。
     * <p>读取一个换行符并生成对应的 {@code NEWLINE} 类型 Token。</p>
     *
     * @param ctx  词法上下文
     * @param line 当前行号
     * @param col  当前列号
     * @return 表示换行的 Token
     */
    @Override
    protected Token scanToken(LexerContext ctx, int line, int col) {
        currentState = State.NEWLINE;

        char first = ctx.peek();
        String lexeme;

        ctx.advance();
        if (first == '\r') {
            // 检查是否是 \r\n
            if (!ctx.isAtEnd() && ctx.peek() == '\n') {
                ctx.advance();
                lexeme = "\r\n";
            } else {
                lexeme = "\r";
            }
        } else {
            // 一定是 \n
            lexeme = "\n";
        }

        Token newlineToken = new Token(TokenType.NEWLINE, lexeme, line, col);
        currentState = State.INITIAL;
        return newlineToken;
    }

    // 定义状态枚举
    private enum State {
        INITIAL,
        NEWLINE
    }
}
