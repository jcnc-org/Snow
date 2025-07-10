package org.jcnc.snow.compiler.parser.context;

/**
 * 表示在语法分析过程中使用了尚未支持的语法或语言特性时抛出的异常。
 * <p>
 * 当用户使用了当前编译器实现尚不支持的语法、关键字或特性时，语法分析器将抛出此异常，用于清晰提示和错误报告。
 * </p>
 */
public final class UnsupportedFeature extends ParseException {

    /**
     * 构造一个“暂未支持的语法/特性”异常。
     *
     * @param feature 未被支持的语法或特性描述
     * @param line    发生异常的行号
     * @param column  发生异常的列号
     */
    public UnsupportedFeature(String feature, int line, int column) {
        super("暂未支持的语法/特性: " + feature, line, column);
    }
}
