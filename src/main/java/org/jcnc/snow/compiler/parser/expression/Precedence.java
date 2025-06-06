package org.jcnc.snow.compiler.parser.expression;

/**
 * {@code Precedence} 表示表达式中各类运算符的优先级枚举。
 * <p>
 * 该优先级枚举用于 Pratt 解析器判断运算符的结合顺序。
 * 枚举顺序即优先级高低，数值越大绑定越紧密。
 * </p>
 */
public enum Precedence {

    /**
     * 最低优先级，通常用于整个表达式解析的起始入口。
     */
    LOWEST,

    /**
     * 加法和减法的优先级（例如 +、-）。
     */
    SUM,

    /**
     * 乘法、除法、取模等更高优先级的二元运算符（例如 *、/、%）。
     */
    PRODUCT,

    /**
     * 函数调用、成员访问等最强绑定（例如 foo()、obj.prop）。
     */
    CALL
}
