package org.jcnc.snow.compiler.cli.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * 版本工具类，用于加载资源文件中的 Snow 编程语言版本信息。
 * <p>
 * 从 classpath 路径下的 version.properties 文件中读取属性 "snow.version"，
 * 并返回对应的版本号字符串。
 * </p>
 */
public class VersionUtils {

    /**
     * 资源文件路径常量，指向 classpath 根目录下的 version.properties 文件。
     */
    private static final String PROPERTIES_PATH = "/version.properties";

    /**
     * 加载并返回 Snow 编程语言的版本号。
     * <p>
     * 通过 Class.getResourceAsStream 方法读取 version.properties 文件，
     * 使用 {@link Properties} 类解析并获取 key 为 "snow.version" 的值。
     * </p>
     *
     * @return 从 version.properties 中读取的版本号（如 "1.0.0"）
     * @throws UncheckedIOException 如果读取或解析配置文件时发生 I/O 错误
     */
    public static String loadVersion() {
        try (InputStream in = VersionUtils.class.getResourceAsStream(PROPERTIES_PATH)) {
            Properties properties = new Properties();
            properties.load(in);
            return properties.getProperty("snow.version");
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load version from " + PROPERTIES_PATH, e);
        }
    }
}
