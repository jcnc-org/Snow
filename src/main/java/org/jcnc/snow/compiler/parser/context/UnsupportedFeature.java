package org.jcnc.snow.compiler.parser.context;

/**
 * 当源码使用了当前编译器尚未支持的语言特性或语法时抛出。
 */
public final class UnsupportedFeature extends ParseException {

    public UnsupportedFeature(String message) {
        super(message);
    }
}
