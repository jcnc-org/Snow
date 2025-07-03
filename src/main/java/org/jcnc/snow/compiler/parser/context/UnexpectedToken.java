package org.jcnc.snow.compiler.parser.context;

/**
 * 当解析过程中遇到意料之外或无法识别的 Token 时抛出。
 */
public final class UnexpectedToken extends ParseException {

    public UnexpectedToken(String message) {
        super(message);
    }
}
