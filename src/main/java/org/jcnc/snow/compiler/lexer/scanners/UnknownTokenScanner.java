package org.jcnc.snow.compiler.lexer.scanners;

import org.jcnc.snow.compiler.lexer.core.LexerContext;
import org.jcnc.snow.compiler.lexer.core.LexicalException;
import org.jcnc.snow.compiler.lexer.token.Token;

/**
 * 未知 Token 扫描器（UnknownTokenScanner）。
 * <p>
 * 作为所有扫描器的兜底处理器。当前字符若不被任何其他扫描器识别，
 * 由本类处理并抛出 {@link LexicalException}，终止词法分析流程。
 * </p>
 * <p>
 * 主要作用：保证所有非法、不可识别的字符（如@、$等）不会被静默跳过或误当作合法 Token，
 * 而是在词法阶段立刻定位并报错，有助于尽早发现源代码问题。
 * </p>
 */
public class UnknownTokenScanner extends AbstractTokenScanner {

    /**
     * 判断是否可以处理当前字符。
     * 对于 UnknownTokenScanner，始终返回 true（兜底扫描器，必须排在扫描器链末尾）。
     * @param c   当前待处理字符
     * @param ctx 词法上下文
     * @return 是否处理该字符（始终为 true）
     */
    @Override
    public boolean canHandle(char c, LexerContext ctx) {
        return true;
    }

    /**
     * 实际处理非法字符序列的方法。
     * 连续读取所有无法被其他扫描器识别的字符，组成错误片段并抛出异常。
     * @param ctx 词法上下文
     * @param line 错误发生行号
     * @param col  错误发生列号
     * @return 不会返回Token（始终抛异常）
     * @throws LexicalException 非法字符导致的词法错误
     */
    @Override
    protected Token scanToken(LexerContext ctx, int line, int col) {
        // 读取一段非法字符（既不是字母数字、也不是常见符号）
        String lexeme = readWhile(ctx, ch ->
                !Character.isLetterOrDigit(ch) &&
                        !Character.isWhitespace(ch) &&
                        ":,().+-*{};\"".indexOf(ch) < 0
        );
        // 如果没读到任何字符，则把当前字符单独作为非法片段
        if (lexeme.isEmpty())
            lexeme = String.valueOf(ctx.advance());
        // 抛出词法异常，并带上错误片段与具体位置
        throw new LexicalException("Illegal character sequence '" + lexeme + "'", line, col);
    }
}
