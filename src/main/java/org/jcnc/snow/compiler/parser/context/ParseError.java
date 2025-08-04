package org.jcnc.snow.compiler.parser.context;

/**
 * 语法错误的数据传输对象（DTO）。
 * <p>
 * 用于收集和展示语法分析过程中检测到的错误信息，便于错误定位和报告。
 * 包含出错文件、行号、列号和具体错误信息等字段。
 * </p>
 */
public class ParseError {

    /** 出错的文件名 */
    private final String file;
    /** 出错的行号 */
    private final int line;
    /** 出错的列号 */
    private final int column;
    /** 错误信息描述 */
    private final String message;

    /**
     * 构造一个语法错误数据对象。
     *
     * @param file    出错文件名
     * @param line    出错行号
     * @param column  出错列号
     * @param message 错误信息描述
     */
    public ParseError(String file, int line, int column, String message) {
        this.file    = file;
        this.line    = line;
        this.column  = column;
        this.message = message;
    }

    /**
     * 返回该错误对象的字符串表示。
     *
     * @return 格式化后的错误描述字符串
     */
    @Override
    public String toString() {
        return "file:///" + file + ":" + line + ":" + column + ": " + message;
    }
}
