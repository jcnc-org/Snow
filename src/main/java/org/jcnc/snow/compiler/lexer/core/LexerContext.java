package org.jcnc.snow.compiler.lexer.core;

import org.jcnc.snow.compiler.lexer.base.TokenScanner;

/**
 * {@code LexerContext} —— 词法分析阶段的上下文状态管理器。<br>
 * <p>
 * 提供对源代码字符流的读取访问、行列号追踪、指针推进与字符匹配等操作，
 * 是 {@link TokenScanner} 实现进行词法识别的基础设施。
 * </p>
 * <p>
 * 设计要点：
 * <ul>
 *     <li>构造时统一将 Windows 换行符 (<code>\r\n</code>) 转换为 Unix 风格 (<code>\n</code>)。</li>
 *     <li>所有坐标均以 <strong>1</strong> 为起始行／列号，更贴合人类直觉。</li>
 *     <li>提供 {@link #peekAhead(int)} 方法以支持“向前多字符查看”而不移动游标。</li>
 * </ul>
 * </p>
 */
public class LexerContext {

    /* ───────────────────────────────── 私有字段 ───────────────────────────────── */

    /** 源代码字符串（换行符已标准化为 \n） */
    private final String source;

    /** 当前扫描位置（自 0 起算的全局偏移量） */
    private int pos = 0;

    /** 当前行号（从 1 开始） */
    private int line = 1;

    /** 当前列号（从 1 开始） */
    private int col = 1;

    /** 上一个字符对应的列号（用于异常定位） */
    private int lastCol = 1;

    /* ──────────────────────────────── 构造 & 基本信息 ─────────────────────────────── */

    /**
     * 创建新的 {@code LexerContext}，并完成换行符标准化。
     *
     * @param rawSource 原始源代码文本
     */
    public LexerContext(String rawSource) {
        this.source = rawSource.replace("\r\n", "\n");
    }

    /**
     * 判断是否已到达源代码结尾。
     *
     * @return 若游标位于终点之后返回 {@code true}
     */
    public boolean isAtEnd() {
        return pos >= source.length();
    }

    /* ──────────────────────────────── 指针推进与查看 ─────────────────────────────── */

    /**
     * 消费 <em>当前</em> 字符并前进一个位置，同时更新行列号。
     *
     * @return 被消费的字符；若已结束则返回空字符 {@code '\0'}
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
     * 查看当前位置字符，但不移动游标。
     *
     * @return 当前字符；若越界则返回 {@code '\0'}
     */
    public char peek() {
        return isAtEnd() ? '\0' : source.charAt(pos);
    }

    /**
     * 查看下一个字符，但不改变位置。
     *
     * @return 下一字符；若越界则返回 {@code '\0'}
     */
    public char peekNext() {
        return pos + 1 >= source.length() ? '\0' : source.charAt(pos + 1);
    }

    /**
     * 向前查看 <em>offset</em> 个字符（不移动游标）。offset=1 等价于 {@link #peekNext()}。
     *
     * @param offset 偏移量 (≥ 1)
     * @return 指定偏移处的字符；若越界返回 {@code '\0'}
     */
    public char peekAhead(int offset) {
        if (offset <= 0) return peek();
        int idx = pos + offset;
        return idx >= source.length() ? '\0' : source.charAt(idx);
    }

    /**
     * 若当前位置字符等于 {@code expected}，则消费并返回 {@code true}；否则保持原位返回 {@code false}。
     *
     * @param expected 期望匹配的字符
     * @return 是否匹配并消费
     */
    public boolean match(char expected) {
        if (isAtEnd() || source.charAt(pos) != expected) return false;
        advance();
        return true;
    }

    /* ──────────────────────────────── 坐标查询 ─────────────────────────────── */

    /** @return 当前行号 (1-based) */
    public int getLine()    { return line; }

    /** @return 当前列号 (1-based) */
    public int getCol()     { return col;  }

    /** @return 上一个字符的列号 */
    public int getLastCol() { return lastCol; }

    /** @return 当前指针在源文件中的全局偏移 (0-based) */
    public int getPos()     { return pos;  }
}
