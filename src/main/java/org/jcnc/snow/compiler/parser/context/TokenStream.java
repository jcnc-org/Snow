package org.jcnc.snow.compiler.parser.context;

import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.lexer.token.TokenType;

import java.util.List;

/**
 * {@code TokenStream} 封装了 Token 序列并维护当前解析位置，是语法分析器读取词法单元的核心工具类。
 * <p>
 * 该类提供前瞻（peek）、消费（next）、匹配（match）、断言（expect）等常用操作，
 * 支持前向查看和异常处理，适用于递归下降等常见语法解析策略。
 * 设计上自动跳过注释（COMMENT）token，并对越界情况提供自动构造的 EOF（文件结束）token，
 * 有效提升语法处理的健壮性与易用性。
 * </p>
 */
public class TokenStream {

    /**
     * 源 Token 列表
     */
    private final List<Token> tokens;
    /**
     * 当前解析位置索引
     */
    private int pos = 0;

    /**
     * 使用 Token 列表构造 TokenStream。
     *
     * @param tokens 词法分析器输出的 Token 集合
     * @throws NullPointerException 如果 tokens 为 null
     */
    public TokenStream(List<Token> tokens) {
        if (tokens == null) {
            throw new NullPointerException("Token 列表不能为空");
        }
        this.tokens = tokens;
    }

    /**
     * 向前查看指定偏移量处的 Token（不移动当前位置）。
     * 在 {@code offset == 0} 时自动跳过所有连续的注释（COMMENT）token。
     *
     * @param offset 相对当前位置的偏移量（0 表示当前位置 token）
     * @return 指定位置的 Token；若越界则返回自动构造的 EOF Token
     */
    public Token peek(int offset) {
        if (offset == 0) {
            skipTrivia();
        }
        int idx = pos + offset;
        if (idx >= tokens.size()) {
            return Token.eof(tokens.size() + 1);
        }
        return tokens.get(idx);
    }

    /**
     * 查看当前位置的有效 Token（已跳过注释）。
     *
     * @return 当前 Token，等效于 {@code peek(0)}
     */
    public Token peek() {
        skipTrivia();
        return peek(0);
    }

    /**
     * 消费当前位置的有效 Token 并前移指针，自动跳过注释 token。
     *
     * @return 被消费的有效 Token
     */
    public Token next() {
        Token t = peek();
        pos++;
        skipTrivia();
        return t;
    }

    /**
     * 若当前 Token 的词素等于指定字符串，则消费该 Token 并前移，否则不变。
     *
     * @param lexeme 目标词素字符串
     * @return 匹配成功返回 true，否则返回 false
     */
    public boolean match(String lexeme) {
        if (peek().getLexeme().equals(lexeme)) {
            next();
            return true;
        }
        return false;
    }

    /**
     * 断言当前位置 Token 的词素等于指定值，否则抛出 {@link ParseException}。
     * 匹配成功时消费该 Token 并前移。
     *
     * @param lexeme 期望的词素字符串
     * @return 匹配成功的 Token
     * @throws ParseException 若词素不匹配
     */
    public Token expect(String lexeme) {
        Token t = peek();
        if (!t.getLexeme().equals(lexeme)) {
            throw new ParseException(
                    "期望的词素是 '" + lexeme + "'，但得到的是 '" + t.getLexeme() + "'",
                    t.getLine(), t.getCol()
            );
        }
        return next();
    }

    /**
     * 断言当前位置 Token 类型为指定类型，否则抛出 {@link ParseException}。
     * 匹配成功时消费该 Token 并前移。
     *
     * @param type 期望的 Token 类型
     * @return 匹配成功的 Token
     * @throws ParseException 若类型不符
     */
    public Token expectType(TokenType type) {
        Token t = peek();
        if (t.getType() != type) {
            throw new ParseException(
                    "期望的标记类型为 " + type + "，但实际得到的是 " + t.getType() +
                            " ('" + t.getLexeme() + "')",
                    t.getLine(), t.getCol()
            );
        }
        return next();
    }

    /**
     * 判断是否已到达文件末尾（EOF）。
     *
     * @return 若当前位置 Token 为 EOF，则返回 true；否则返回 false
     */
    public boolean isAtEnd() {
        return peek().getType() == TokenType.EOF;
    }

    /**
     * 跳过所有连续的注释（COMMENT）token，使解析器总是定位在第一个有效 Token 上。
     */
    private void skipTrivia() {
        while (pos < tokens.size()
                && tokens.get(pos).getType() == TokenType.COMMENT) {
            pos++;
        }
    }
}
