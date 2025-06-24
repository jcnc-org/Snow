package org.jcnc.snow.pkg.utils;

/**
 * 提供示例 .snow 模块代码模板。
 */
public final class SnowExampleTemplate {

    private SnowExampleTemplate() {
        // 工具类不允许实例化
    }

    /**
     * 返回 main.snow 示例模块内容。
     *
     * @return 示例模块代码字符串
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
                        declare n:int
                    return_type: int
                    body:
                        declare num1:int = 1
                        loop:
                            initializer:
                                declare counter:int = 1
                            condition:
                                counter <= n
                            update:
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
