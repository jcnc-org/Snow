package org.jcnc.snow.compiler.lexer.scanners;

import org.jcnc.snow.compiler.lexer.core.LexerContext;
import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.lexer.token.TokenType;

/**
 * 换行符扫描器：将源代码中的换行符（\n）识别为 {@code NEWLINE} 类型的 Token。
 * <p>
 * 通常用于记录行的分界，辅助语法分析阶段进行行敏感的判断或保持结构清晰。
 */
public class NewlineTokenScanner extends AbstractTokenScanner {

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
        return c == '\n';
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
        ctx.advance();
        return new Token(TokenType.NEWLINE, "\n", line, col);
    }
}