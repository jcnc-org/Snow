package org.jcnc.snow.compiler.parser.context;

/**
 * 表示在语法分析过程中遇到意料之外或无法识别的 Token 时抛出的异常。
 * <p>
 * 当分析器检测到实际遇到的 Token 不符合语法规则，或与预期类型不符时会抛出本异常，便于错误定位和报告。
 * </p>
 */
public final class UnexpectedToken extends ParseException {

    /**
     * 构造一个“意外的 Token”异常。
     *
     * @param actual 实际遇到的 Token 描述
     * @param line   发生异常的行号
     * @param column 发生异常的列号
     */
    public UnexpectedToken(String actual, int line, int column) {
        super("意外的 Token: " + actual, line, column);
    }
}
