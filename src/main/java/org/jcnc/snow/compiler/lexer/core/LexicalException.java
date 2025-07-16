package org.jcnc.snow.compiler.lexer.core;

/**
 * 词法异常（LexicalException）。
 * <p>
 * 当 {@link LexerEngine} 在扫描过程中遇到
 * 非法或无法识别的字符序列时抛出该异常。
 * <ul>
 *   <li>异常消息仅包含一行简明错误信息（包含行号与列号）；</li>
 *   <li>完全禁止 Java 堆栈信息输出，使命令行输出保持整洁。</li>
 * </ul>
 * <pre>
 * 例：
 *     Main.snow: 行 7, 列 20: 词法错误：非法字符序列 '@'
 * </pre>
 */
public class LexicalException extends RuntimeException {
    /** 错误发生的行号（从1开始） */
    private final int line;
    /** 错误发生的列号（从1开始） */
    private final int column;
    /** 错误原因 */
    private final String reason;

    /**
     * 构造词法异常
     * @param reason  错误原因（如：非法字符描述）
     * @param line    出错行号
     * @param column  出错列号
     */
    public LexicalException(String reason, int line, int column) {
        // 错误描述直接为 reason，禁止异常堆栈打印
        super(reason, null, false, false);
        this.reason = reason;
        this.line   = line;
        this.column = column;
    }


    /**
     * 屏蔽异常堆栈填充（始终不打印堆栈信息）
     */
    @Override
    public synchronized Throwable fillInStackTrace() { return this; }

    /**
     * 获取出错的行号
     * @return 行号
     */
    public int getLine()   { return line; }

    /**
     * 获取出错的列号
     * @return 列号
     */
    public int getColumn() { return column; }

    /**
     * 获取出错的描述
     * @return 出错描述
     */
    public String getReason() { return reason; }
}
