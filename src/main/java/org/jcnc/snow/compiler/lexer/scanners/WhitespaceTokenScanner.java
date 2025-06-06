package org.jcnc.snow.compiler.lexer.scanners;

import org.jcnc.snow.compiler.lexer.core.LexerContext;
import org.jcnc.snow.compiler.lexer.token.Token;

/**
 * 空白符扫描器：跳过非换行的空白字符，不生成任何 Token。
 * <p>
 * 支持的空白字符包括空格、制表符（Tab）等，但不包括换行符（由 {@link NewlineTokenScanner} 处理）。
 * <p>
 * 此扫描器仅用于忽略多余的空白内容，保持 Token 流的紧凑性。
 */
public class WhitespaceTokenScanner extends AbstractTokenScanner {

    /**
     * 判断是否可以处理当前位置的字符。
     * <p>当字符为空白符但不是换行符（\n）时返回 true。</p>
     *
     * @param c   当前字符
     * @param ctx 当前词法上下文
     * @return 如果为非换行的空白字符，则返回 true
     */
    @Override
    public boolean canHandle(char c, LexerContext ctx) {
        return Character.isWhitespace(c) && c != '\n';
    }

    /**
     * 跳过空白字符，不生成 Token。
     * <p>直接推进上下文位置，返回 null。</p>
     *
     * @param ctx  词法上下文
     * @param line 当前行号
     * @param col  当前列号
     * @return 始终返回 null（表示无 Token 产生）
     */
    @Override
    protected Token scanToken(LexerContext ctx, int line, int col) {
        ctx.advance();
        return null;
    }
}
