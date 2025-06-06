package org.jcnc.snow.compiler.lexer.core;

import org.jcnc.snow.compiler.lexer.base.TokenScanner;

/**
 * {@code LexerContext} 是词法分析阶段的上下文状态管理器。
 * <p>
 * 该类提供对源代码字符流的读取访问，追踪当前行号与列号，
 * 并支持字符匹配、回看与指针推进等操作，是 {@link TokenScanner} 实现进行词法识别的重要支撑工具。
 * </p>
 * <p>
 * 所有源代码输入在构造时统一将 Windows 风格的换行符（\r\n）转换为 Unix 风格（\n），
 * 保证换行行为一致性。
 * </p>
 */
public class LexerContext {

    /** 源代码字符串，换行符已标准化为 \n */
    private final String source;

    /** 当前扫描位置（从 0 开始的偏移） */
    private int pos = 0;

    /** 当前行号，从 1 开始 */
    private int line = 1;

    /** 当前列号，从 1 开始 */
    private int col = 1;

    /** 上一个字符对应的列号（用于位置精确记录） */
    private int lastCol = 1;

    /**
     * 构造一个新的 {@code LexerContext} 实例，并标准化换行符。
     *
     * @param source 原始源代码字符串
     */
    public LexerContext(String source) {
        this.source = source.replace("\r\n", "\n");
    }

    /**
     * 判断是否已读取到源代码末尾。
     *
     * @return 若已结束，返回 {@code true}；否则返回 {@code false}
     */
    public boolean isAtEnd() {
        return pos >= source.length();
    }

    /**
     * 消费当前字符并前进一个位置，自动更新行列信息。
     *
     * @return 当前字符，若已结束则返回空字符（'\0'）
     */
    public char advance() {
        if (isAtEnd()) return '\0';
        char c = source.charAt(pos++);
        lastCol = col;
        if (c == '\n') {
            line++;
            col = 1;
        } else {
            col++;
        }
        return c;
    }

    /**
     * 查看当前位置的字符，但不前进。
     *
     * @return 当前字符，若结束则返回空字符
     */
    public char peek() {
        return isAtEnd() ? '\0' : source.charAt(pos);
    }

    /**
     * 查看下一个字符，但不改变位置。
     *
     * @return 下一个字符，若结束则返回空字符
     */
    public char peekNext() {
        return pos + 1 >= source.length() ? '\0' : source.charAt(pos + 1);
    }

    /**
     * 若当前字符与期望字符相同，则前进并返回 {@code true}，否则不动并返回 {@code false}。
     *
     * @param expected 期待匹配的字符
     * @return 是否匹配成功并消费
     */
    public boolean match(char expected) {
        if (isAtEnd() || source.charAt(pos) != expected) return false;
        advance();
        return true;
    }

    /**
     * 获取当前位置的行号。
     *
     * @return 当前行号（从 1 开始）
     */
    public int getLine() {
        return line;
    }

    /**
     * 获取当前位置的列号。
     *
     * @return 当前列号（从 1 开始）
     */
    public int getCol() {
        return col;
    }

    /**
     * 获取上一个字符所在的列号。
     *
     * @return 上一个字符对应的列位置
     */
    public int getLastCol() {
        return lastCol;
    }
}
