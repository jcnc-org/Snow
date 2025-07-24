package org.jcnc.snow.common;

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
}
