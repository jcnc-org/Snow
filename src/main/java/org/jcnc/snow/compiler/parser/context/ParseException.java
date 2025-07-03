package org.jcnc.snow.compiler.parser.context;

/**
 * {@code ParseException}——语法分析阶段所有错误的基类。
 *
 * <p>声明为 <em>sealed</em>，仅允许 {@link UnexpectedToken}、
 * {@link MissingToken}、{@link UnsupportedFeature} 三个受信子类继承，
 * 以便调用方根据异常类型进行精确处理。</p>
 */
public sealed class ParseException extends RuntimeException
        permits UnexpectedToken, MissingToken, UnsupportedFeature {

    /**
     * 构造解析异常并附带错误消息。
     *
     * @param message 错误描述
     */
    public ParseException(String message) {
        super(message);
    }
}
