package org.jcnc.snow.compiler.lexer.core;

/**
 * 表示词法分析过程中发生的错误信息。
 * <p>
 * 该类用于封装词法分析（lexical analysis）阶段发现的错误，包括错误位置（文件名、行号、列号）
 * 以及错误描述信息，便于定位和调试。
 * </p>
 */
public class LexicalError {
    /**
     * 出错所在的源文件名。
     */
    private final String file;

    /**
     * 出错所在的行号（从1开始）。
     */
    private final int line;

    /**
     * 出错所在的列号（从1开始）。
     */
    private final int column;

    /**
     * 错误的详细描述信息。
     */
    private final String message;

    /**
     * 构造一个新的 {@code LexicalError} 实例。
     *
     * @param file    出错的源文件名
     * @param line    出错所在的行号（从1开始）
     * @param column  出错所在的列号（从1开始）
     * @param message 错误的详细描述信息
     */
    public LexicalError(String file, int line, int column, String message) {
        this.file = file;
        this.line = line;
        this.column = column;
        this.message = message;
    }

    /**
     * 以易于阅读的字符串形式返回错误信息。
     *
     * @return 格式化的错误信息字符串，包含文件名、行号、列号和错误描述
     */
    @Override
    public String toString() {
        return file + ":" + line + ":" + column + ": " + message;
    }
}
