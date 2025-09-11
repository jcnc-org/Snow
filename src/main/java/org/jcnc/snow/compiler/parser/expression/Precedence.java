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
     * 逻辑或（||）
     */
    OR,

    /**
     * 逻辑与（&&）
     */
    AND,

    /**
     * 相等/不等（==, !=）
     */
    EQUALITY,

    /**
     * 大小比较（<, >, <=, >=）
     */
    COMPARISON,

    /**
     * 加法和减法（+、-）
     */
    SUM,

    /**
     * 乘法、除法、取模（*、/、%）
     */
    PRODUCT,

    /**
     * 一元前缀（-x !x）
     */
    UNARY,

    /**
     * 函数调用、成员访问等最强绑定（foo()、obj.prop）
     */
    CALL
}
