package org.jcnc.snow.compiler.parser.context;

/**
 * 当语法结构缺失必须出现的 Token 时抛出。
 */
public final class MissingToken extends ParseException {

    public MissingToken(String message) {
        super(message);
    }
}
