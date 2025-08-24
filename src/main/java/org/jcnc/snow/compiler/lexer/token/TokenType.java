package org.jcnc.snow.compiler.lexer.token;

/**
 * {@code TokenType} 枚举定义了 Snow 编程语言词法分析阶段可识别的所有词法单元类型。
 * <p>
 * 每个枚举值代表一种语义类别，
 * 用于描述源代码中出现的关键字、标识符、字面量、运算符、控制符号等语言构件。
 * 在语法分析及后续处理阶段，依赖该枚举对 Token 进行分类、判断与分支处理。
 * </p>
 */
public enum TokenType {

    /* ---------- 基础 ---------- */
    /** 普通标识符，如变量名、函数名等 */
    IDENTIFIER,

    /** 关键字（declare、if、else、loop、break、continue、return 等） */
    KEYWORD,

    /** 内置类型名（byte、short、int、long、float、double、string、boolean、void 等） */
    TYPE,

    /* ---------- 字面量 ---------- */
    /** 布尔字面量 （true / false） */
    BOOL_LITERAL,

    /** 字符串字面量（如 "hello"） */
    STRING_LITERAL,

    /** 数字字面量（整数或浮点数） */
    NUMBER_LITERAL,

    /* ---------- 分隔符 ---------- */
    /** 冒号 ':' */
    COLON,

    /** 逗号 ',' */
    COMMA,

    /** 点号 '.' */
    DOT,

    /* ---------- 运算符 ---------- */
    /** 赋值符号 '=' */
    EQUALS,

    /** 取模运算符 '%' */
    MODULO,

    /** 加号 '+' */
    PLUS,

    /** 乘号 '*' */
    MULTIPLY,
    /**除号 '/' */
    DIVIDE,

    /** 减号 '-' */
    MINUS,

    /** 取反 '!' */
    NOT,

    /** 左括号 '(' */
    LPAREN,

    /** 右括号 ')' */
    RPAREN,

    /** 左中括号 '[' */
    LBRACKET,

    /** 右中括号 ']' */
    RBRACKET,

    /** 相等比较符号 '==' */
    DOUBLE_EQUALS,

    /** 不等比较符号 '!=' */
    NOT_EQUALS,

    /** 大于符号 '>' */
    GREATER_THAN,

    /** 大于等于符号 '>=' */
    GREATER_EQUAL,

    /** 小于符号 '<' */
    LESS_THAN,

    /** 小于等于符号 '<=' */
    LESS_EQUAL,

    /** 逻辑与符号 '&&' */
    AND,

    /** 逻辑或符号 '||' */
    OR,

    /** 换行符，标识逻辑换行 */
    NEWLINE,

    /** 单行或多行注释内容 */
    COMMENT,

    /** 文件结束符（End of File） */
    EOF,

    /** 无法识别的非法或未知符号 */
    UNKNOWN
}