package org.jcnc.snow.pkg.utils;

/**
 * 示例模块模板工具类，提供标准的示例 Snow 代码片段。
 * <p>
 * 用于项目脚手架生成或帮助用户快速上手 .snow 语言。
 * </p>
 */
public final class SnowExample {
    /**
     * 获取 main.snow 示例模块的内容字符串。
     *
     * @return main.snow 示例模块的完整代码
     */
    public static String getMainModule() {
        return """
                module: main
                    import: std_io
                    function: main
                        returns: void
                        body:
                            std_io.println("Hello" + " " + "World!")
                        end body
                    end function
                end module
                """;
    }
}
