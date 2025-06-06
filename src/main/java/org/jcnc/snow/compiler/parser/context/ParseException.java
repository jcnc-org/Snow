package org.jcnc.snow.compiler.parser.context;

/**
 * {@code ParseException} 表示语法分析阶段发生的错误。
 * <p>
 * 当语法分析器遇到非法的语法结构或无法继续处理的标记序列时，
 * 应抛出该异常以中断当前解析流程，并向调用方报告错误信息。
 * </p>
 * <p>
 * 该异常通常由 {@code ParserContext} 或各类语法规则处理器主动抛出，
 * 用于提示编译器前端或 IDE 系统进行错误提示与恢复。
 * </p>
 */
public class ParseException extends RuntimeException {

    /**
     * 构造一个带有错误描述信息的解析异常实例。
     *
     * @param message 错误描述文本，用于指明具体的语法错误原因
     */
    public ParseException(String message) {
        super(message);
    }
}
