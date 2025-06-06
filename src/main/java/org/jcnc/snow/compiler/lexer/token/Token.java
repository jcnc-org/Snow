package org.jcnc.snow.compiler.lexer.token;

/**
 * {@code Token} 表示词法分析过程中生成的最小语法单元，
 * 包含类型信息、词素内容、源代码中对应的原始文本片段以及精确的位置信息。
 * <p>
 * 一个 Token 通常对应源代码中一个具有语义意义的片段，如关键字、标识符、常量、运算符等。
 * 区分 lexeme（清洗后的词素）与 raw（原始片段）是为了支持如带引号的字符串、注释等需要保留原始形式的元素。
 * </p>
 */
public class Token {

    /** Token 的类型，如 KEYWORD、IDENTIFIER、TYPE 等。 */
    private final TokenType type;

    /** 清洗后的词素内容，例如去掉引号的字符串正文或注释正文。 */
    private final String lexeme;

    /** 源代码中对应的原始片段，可能包含引号、注释符号等。 */
    private final String raw;

    /** Token 在源文件中的行号，从 1 开始计数。 */
    private final int line;

    /** Token 在源文件行中的列号，从 1 开始计数。 */
    private final int col;

    /**
     * 构造一个完整信息的 Token 实例。
     *
     * @param type   Token 类型
     * @param lexeme 清洗后的词素内容
     * @param raw    源代码中的原始片段
     * @param line   所在源文件的行号（从 1 开始）
     * @param col    所在源文件行的列号（从 1 开始）
     */
    public Token(TokenType type, String lexeme, String raw, int line, int col) {
        this.type = type;
        this.lexeme = lexeme;
        this.raw = raw;
        this.line = line;
        this.col = col;
    }

    /**
     * 构造一个简化形式的 Token，词素与原始片段一致。
     * <p>
     * 适用于标识符、关键字、符号等不需要区分原始与清洗内容的 Token。
     * </p>
     *
     * @param type   Token 类型
     * @param lexeme Token 内容（同时作为原始片段）
     * @param line   行号
     * @param col    列号
     */
    public Token(TokenType type, String lexeme, int line, int col) {
        this(type, lexeme, lexeme, line, col);
    }

    /**
     * 构造并返回一个表示文件结束（EOF）的 Token 实例。
     * <p>
     * 用于表示扫描结束的特殊符号。
     * </p>
     *
     * @param line 文件结束所在行号
     * @return EOF 类型的 Token
     */
    public static Token eof(int line) {
        return new Token(TokenType.EOF, "", "", line, 1);
    }

    /** @return 此 Token 的类型 */
    public TokenType getType() {
        return type;
    }

    /** @return 清洗后的词素内容（lexeme） */
    public String getLexeme() {
        return lexeme;
    }

    /** @return 源代码中的原始片段 */
    public String getRaw() {
        return raw;
    }

    /** @return Token 所在的源文件行号（从 1 开始） */
    public int getLine() {
        return line;
    }

    /** @return Token 所在行的列号（从 1 开始） */
    public int getCol() {
        return col;
    }

    /**
     * 返回该 Token 的字符串表示，包含类型、词素、行列信息。
     * <p>
     * 通常用于日志打印或调试目的。
     * </p>
     *
     * @return Token 的描述性字符串
     */
    @Override
    public String toString() {
        return String.format(
                "Token(type=%s, lexeme='%s', line=%d, col=%d)",
                type, lexeme, line, col
        );
    }
}