package org.jcnc.snow.common;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 全局编译/运行配置和调试输出工具类。
 * <p>
 * 用于统一设置程序的运行模式，并在调试模式下输出日志信息。
 * </p>
 */
public final class SnowConfig {

    /**
     * 当前运行模式，默认为 {@link Mode#RUN}。
     */
    public static Mode MODE = Mode.RUN;

    /**
     * 标准库路径，默认为项目根目录下的 lib 文件夹
     * <p>
     * 查找优先级：
     * 1. 环境变量 SNOW_LIB
     * 2. 系统属性 snow.lib
     * 3. 项目目录下的 lib 文件夹
     * 4. Snow SDK安装目录下的 lib 文件夹 (通过SNOW_HOME环境变量或snow.home系统属性)
     * 5. 可执行文件所在目录推断的SDK目录
     */
    public static Path STDLIB_PATH = Paths.get("lib").toAbsolutePath();

    /**
     * 私有构造方法，防止实例化。
     */
    private SnowConfig() {
    }

    /**
     * 仅在 {@link Mode#DEBUG} 模式下打印一行信息。
     *
     * @param msg 要输出的信息
     */
    public static void print(String msg) {
        if (MODE == Mode.DEBUG) System.out.println(msg);
    }

    /**
     * 仅在 {@link Mode#DEBUG} 模式下按格式输出信息。
     *
     * @param fmt  格式化字符串
     * @param args 格式化参数
     */
    public static void print(String fmt, Object... args) {
        if (MODE == Mode.DEBUG) System.out.printf(fmt, args);
    }

    /**
     * 判断当前是否为 {@link Mode#DEBUG} 模式。
     *
     * @return 当且仅当当前为调试模式时返回 true
     */
    public static boolean isDebug() {
        return MODE == Mode.DEBUG;
    }

    /**
     * 判断当前是否为 {@link Mode#RUN} 模式。
     *
     * @return 当且仅当当前为运行模式时返回 true
     */
    public static boolean isRun() {
        return MODE == Mode.RUN;
    }

    /**
     * 获取标准库路径。
     *
     * @return 标准库路径
     */
    public static Path getStdlibPath() {
        return STDLIB_PATH;
    }

    /**
     * 设置标准库路径。
     *
     * @param path 标准库路径
     */
    public static void setStdlibPath(Path path) {
        STDLIB_PATH = path.toAbsolutePath();
    }
}
