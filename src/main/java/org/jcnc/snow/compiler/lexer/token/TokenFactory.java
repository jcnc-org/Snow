package org.jcnc.snow.compiler.lexer.token;

import java.util.Set;

/**
 * {@code TokenFactory} 是一个用于词法分析的静态工厂类。
 * <p>
 * 该类提供静态方法，根据输入的原始词素（字符串），
 * 自动识别其对应的词法类型，并生成相应的 {@link Token} 实例。
 * 支持自动区分语言关键字、内置类型、合法标识符等，
 * 简化了词法扫描器（Lexer）的核心处理流程。
 * </p>
 *
 * <p>
 * 主要功能与特性：
 * <ul>
 *     <li>统一管理语言关键字和类型名集合，便于扩展与维护。</li>
 *     <li>自动推断 Token 类型，无需外部干预。</li>
 *     <li>对不合法的词素自动标记为 UNKNOWN 类型。</li>
 * </ul>
 * </p>
 *
 * @author 你的名字
 * @version 1.0
 */
public class TokenFactory {

    /**
     * 语言的保留关键字集合。
     */
    private static final Set<String> KEYWORDS = Set.of(
            "module", "function", "parameter", "return_type",
            "body", "end", "if", "then", "else", "loop",
            "declare", "return", "import",
            "initializer", "condition", "update"
    );

    /**
     * 内置类型名称集合，如 int、string 等。
     */
    private static final Set<String> TYPES = Set.of(
            "int", "string", "float", "bool", "void", "double", "long", "short", "byte"
    );

    /**
     * 创建一个根据内容自动推断类型的 {@link Token} 实例。
     * <p>
     * 优先级顺序为：类型（TYPE） &gt; 关键字（KEYWORD） &gt; 标识符（IDENTIFIER） &gt; 未知（UNKNOWN）。
     * 若原始字符串同时属于多类，则按优先级最高者处理。
     * </p>
     *
     * @param raw  原始词法单元文本
     * @param line Token 所在行号（从1开始）
     * @param col  Token 所在列号（从1开始）
     * @return 构造好的 {@link Token} 实例
     */
    public static Token create(String raw, int line, int col) {
        TokenType type = determineTokenType(raw);
        return new Token(type, raw, line, col);
    }

    /**
     * 判断并推断给定字符串的 {@link TokenType} 类型。
     * <p>
     * 优先级依次为：TYPE &gt; KEYWORD &gt; IDENTIFIER &gt; UNKNOWN。
     * </p>
     *
     * @param raw 原始词素字符串
     * @return 推断出的 {@link TokenType} 类型
     */
    private static TokenType determineTokenType(String raw) {
        if (isType(raw)) return TokenType.TYPE;
        if (isKeyword(raw)) return TokenType.KEYWORD;
        if (isIdentifier(raw)) return TokenType.IDENTIFIER;
        return TokenType.UNKNOWN;
    }

    /**
     * 判断指定字符串是否为内置类型标识。
     *
     * @param raw 输入的字符串
     * @return 若为类型名则返回 {@code true}，否则返回 {@code false}
     */
    private static boolean isType(String raw) {
        return TYPES.contains(raw);
    }

    /**
     * 判断指定字符串是否为语言保留关键字。
     *
     * @param raw 输入的字符串
     * @return 若为关键字则返回 {@code true}，否则返回 {@code false}
     */
    private static boolean isKeyword(String raw) {
        return KEYWORDS.contains(raw);
    }

    /**
     * 判断指定字符串是否为合法的标识符（Identifier）。
     * <p>
     * 合法标识符需以字母（a-z/A-Z）或下划线（_）开头，
     * 后续可包含字母、数字或下划线。
     * 例如：_abc, a1b2, name_123 均为合法标识符。
     * </p>
     *
     * @param raw 输入的字符串
     * @return 若为合法标识符则返回 {@code true}，否则返回 {@code false}
     */
    private static boolean isIdentifier(String raw) {
        return raw.matches("[a-zA-Z_][a-zA-Z0-9_]*");
    }
}
