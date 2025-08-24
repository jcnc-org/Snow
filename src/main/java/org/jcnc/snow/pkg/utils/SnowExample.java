package org.jcnc.snow.pkg.utils;

/**
 * 示例模块模板工具类，提供标准的示例 Snow 代码片段。
 * <p>
 * 用于项目脚手架生成或帮助用户快速上手 .snow 语言。
 * </p>
 */
public final class SnowExample {

    /**
     * 工具类构造方法，禁止实例化。
     */
    private SnowExample() {
        // 工具类不允许实例化
    }

    /**
     * 获取 main.snow 示例模块的内容字符串。
     *
     * @return main.snow 示例模块的完整代码
     */
    public static String getMainModule() {
        return """
                module: Math
                    import: os
                    function: main
                        returns: void
                        body:
                            os.print(Math.factorial(6))
                        end body
                    end function
                
                    function: factorial
                        params:
                            declare n: int
                        returns: int
                        body:
                            declare num1: int = 1
                            loop:
                                init:
                                    declare counter:int = 1
                                cond:
                                    counter <= n
                                step:
                                    counter = counter + 1
                                body:
                                    num1 = num1 * counter
                                end body
                            end loop
                            return num1
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
                    import: os
                    function: print
                        params:
                            declare i1: int
                        returns: void
                        body:
                            syscall("PRINT", i1)
                        end body
                    end function
                end module
                """;
    }
}
