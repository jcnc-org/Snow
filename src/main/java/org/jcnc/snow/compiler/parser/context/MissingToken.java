package org.jcnc.snow.compiler.parser.context;

/**
 * 表示在语法分析过程中，必须出现的 Token 缺失时抛出的异常。
 * <p>
 * 当分析器检测到输入流中缺少某个预期 Token 时，会抛出此异常，以便准确地指明语法错误位置。
 * 该异常包含了缺失 Token 的名称以及发生缺失的位置（行号和列号），便于错误定位和后续处理。
 * </p>
 */
public final class MissingToken extends ParseException {

    /**
     * 构造一个表示缺失 Token 的异常。
     *
     * @param expected 预期但未出现的 Token 名称
     * @param line     发生异常的行号
     * @param column   发生异常的列号
     */
    public MissingToken(String expected, int line, int column) {
        super("缺失 Token: " + expected, line, column);
    }
}
