package org.jcnc.snow.pkg.utils;

/**
 * 示例模块模板工具类，提供 main.snow 的标准示例代码字符串。
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
                    function: main
                        parameter:
                        return_type: int
                        body:
                            Math.factorial(6)
                            return 0
                        end body
                    end function
                
                    function: factorial
                        parameter:
                            declare n: int
                        return_type: int
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
}
