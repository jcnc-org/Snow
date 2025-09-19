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
                    import: os
                    function: main
                        returns: void
                        body:
                            os.print("Hello" + " "+"World!")
                        end body
                    end function
                end module
                """;
    }

    /**
     * 获取系统库 os.snow 模块的内容字符串。
     *
     * @return os.snow 模块的完整代码
     */
    public static String getOsModule() {
        return """
                module: os
                    function: print
                        params:
                            declare i1: any
                        returns: void
                        body:
                            syscall("PRINT", i1)
                        end body
                    end function
                    function: println
                        params:
                            declare i1: any
                        returns: void
                        body:
                            syscall("PRINTLN", i1)
                        end body
                    end function
                end module
                """;
    }
}
