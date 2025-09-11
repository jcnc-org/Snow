package org.jcnc.snow.compiler.parser.context;

/**
 * 语法分析阶段所有错误的基类。
 * <p>
 * 本异常作为语法分析相关错误的统一父类，屏蔽了堆栈信息，确保在命令行界面（CLI）输出时只占用一行，方便用户快速定位问题。
 * 通过 {@code permits} 关键字，限定了可被继承的异常类型，增强类型安全性。
 * </p>
 *
 * <p>
 * 该异常携带错误发生的行号、列号和具体原因信息，用于语法错误的精确报告和输出展示。
 * </p>
 */
public sealed class ParseException extends RuntimeException
        permits MissingToken, UnexpectedToken, UnsupportedFeature {

    /**
     * 出错行号（从 1 开始）
     */
    private final int line;
    /**
     * 出错列号（从 1 开始）
     */
    private final int column;
    /**
     * 错误原因描述
     */
    private final String reason;

    /**
     * 构造语法分析异常。
     *
     * @param reason 错误原因描述
     * @param line   出错行号（从 1 开始）
     * @param column 出错列号（从 1 开始）
     */
    public ParseException(String reason, int line, int column) {
        // 禁用 cause / suppression / stackTrace，确保 CLI 输出简洁
        super(reason, null, false, false);
        this.reason = reason;
        this.line = line;
        this.column = column;
    }

    /**
     * 禁用堆栈信息的生成，保证异常始终为单行输出。
     *
     * @return 当前异常对象自身
     */
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

    /**
     * 获取出错行号（从 1 开始）。
     *
     * @return 行号
     */
    public int getLine() {
        return line;
    }

    /**
     * 获取出错列号（从 1 开始）。
     *
     * @return 列号
     */
    public int getColumn() {
        return column;
    }

    /**
     * 获取错误原因描述。
     *
     * @return 错误原因
     */
    public String getReason() {
        return reason;
    }
}
