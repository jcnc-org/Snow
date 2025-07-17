package org.jcnc.snow.compiler.lexer.scanners;

import org.jcnc.snow.compiler.lexer.core.LexerContext;
import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.lexer.token.TokenType;

/**
 * 字符串扫描器（StringTokenScanner）用于处理由双引号包裹的字符串字面量，
 * 并支持常见转义字符的解析。该扫描器采用有限状态机（状态机）实现，
 * 保证对各类字符串格式进行准确的词法分析。
 * <p>
 * 主要支持如下字符串样式:
 * <ul>
 *   <li>"hello"</li>
 *   <li>"line\\nbreak"</li>
 *   <li>"escaped \\\" quote"</li>
 * </ul>
 * <p>
 * 扫描器会保留字符串的原始形式（含双引号和转义符），
 * 并返回{@link TokenType#STRING_LITERAL}类型的Token。
 * </p>
 * <p>
 * 状态机说明：
 * <ul>
 *   <li>START：起始状态，准备扫描第一个双引号</li>
 *   <li>STRING：扫描字符串内容</li>
 *   <li>ESCAPE：处理转义字符</li>
 * </ul>
 *
 * @author 你的名字
 * @since 2024
 */
public class StringTokenScanner extends AbstractTokenScanner {

    /**
     * 判断当前位置的字符是否为字符串起始符号。
     * <p>
     * 只有遇到双引号（"）时，才由本扫描器处理。
     * </p>
     *
     * @param c   当前待扫描字符
     * @param ctx 当前词法分析上下文
     * @return 如果是字符串起始符号，返回 true，否则返回 false
     */
    @Override
    public boolean canHandle(char c, LexerContext ctx) {
        return c == '"';  // 只处理双引号起始
    }

    /**
     * 执行字符串字面量的具体扫描过程。采用有限状态机处理逻辑，
     * 从起始的双引号开始，逐字符处理字符串内容，支持基本转义序列（如 \\n、\\t、\\\" 等）。
     * <p>
     * 扫描遇到未转义的结尾双引号时即结束，并立即返回Token。
     * 如果遇到换行或文件结束但未遇到结尾引号，视为字符串未闭合，仍返回已扫描内容。
     * </p>
     *
     * @param ctx  词法分析上下文，支持字符遍历与回退等操作
     * @param line 字符串开始行号（用于错误定位）
     * @param col  字符串开始列号（用于错误定位）
     * @return 解析得到的字符串字面量类型Token
     */
    @Override
    protected Token scanToken(LexerContext ctx, int line, int col) {
        StringBuilder sb = new StringBuilder(); // 用于收集字符串原文
        State currentState = State.START;       // 初始状态为START

        // 主循环，直到文件结束或状态机中止
        while (!ctx.isAtEnd()) {
            char c = ctx.advance(); // 消耗当前字符
            sb.append(c);

            switch (currentState) {
                case START:
                    // 第一个双引号，状态切换到STRING
                    currentState = State.STRING;
                    break;

                case STRING:
                    if (c == '\\') {
                        // 遇到转义符，切换到ESCAPE状态
                        currentState = State.ESCAPE;
                    } else if (c == '"') {
                        // 遇到结束双引号，立即返回Token（字符串扫描完毕）
                        return new Token(TokenType.STRING_LITERAL, sb.toString(), line, col);
                    } else if (c == '\n' || c == '\r') {
                        // 若字符串未闭合且遇到换行，提前返回（可根据需要抛异常或报错）
                        return new Token(TokenType.STRING_LITERAL, sb.toString(), line, col);
                    }
                    // 其他字符，保持在STRING状态继续扫描
                    break;

                case ESCAPE:
                    // ESCAPE状态：下一个字符会作为转义内容，无论是"、n、t等
                    // 注意advance已经处理，所以不需要再append
                    currentState = State.STRING;
                    break;
            }
        }

        // 若扫描到文件尾仍未遇到结尾引号，则返回当前内容
        return new Token(TokenType.STRING_LITERAL, sb.toString(), line, col);
    }

    /**
     * 状态机枚举类型，表示当前字符串解析所处的状态。
     */
    private enum State {
        /**
         * 起始状态，等待遇到第一个双引号
         */
        START,
        /**
         * 字符串内容状态，处理实际字符串及普通字符
         */
        STRING,
        /**
         * 处理转义序列状态，遇到'\\'后切换到此状态
         */
        ESCAPE,
    }
}
