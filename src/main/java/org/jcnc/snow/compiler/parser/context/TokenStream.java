package org.jcnc.snow.compiler.parser.context;

import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.lexer.token.TokenType;

import java.util.List;

/**
 * {@code TokenStream} 封装了一个 Token 列表并维护当前解析位置，是语法分析器读取词法单元的核心工具类。
 *
 * <p>提供前瞻（peek）、消费（next）、匹配（match）、断言（expect）等常用操作，
 * 支持前向查看和异常处理，适用于递归下降解析等常见语法构建策略。</p>
 *
 */
public class TokenStream {

    /**
     * 源 Token 列表。
     */
    private final List<Token> tokens;

    /**
     * 当前解析位置索引。
     */
    private int pos = 0;

    /**
     * 使用 Token 列表构造 TokenStream。
     *
     * @param tokens 由词法分析器产生的 Token 集合
     * @throws NullPointerException 如果 tokens 为 null
     */
    public TokenStream(List<Token> tokens) {
        if (tokens == null) {
            throw new NullPointerException("Token list cannot be null.");
        }
        this.tokens = tokens;
    }

    /**
     * 向前查看指定偏移量处的 Token（不移动位置）。
     * 会在 offset==0 时自动跳过当前位置的所有注释（COMMENT）token。
     *
     * @param offset 相对当前位置的偏移量（0 表示当前 token）
     * @return 指定位置的 Token；若越界则返回自动构造的 EOF Token
     */
    public Token peek(int offset) {
        // 只在 offset==0 时跳注释，向前多步 peek 由调用方控制
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
     * 查看当前位置的 Token，等效于 {@code peek(0)}。
     *
     * @return 当前有效 Token（已跳过注释）
     */
    public Token peek() {
        skipTrivia();
        return peek(0);
    }

    /**
     * 消费当前位置的 Token 并返回，位置前移。注释 token 会被自动跳过。
     *
     * @return 被消费的有效 Token（已跳过注释）
     */
    public Token next() {
        Token t = peek();   // peek() 已跳过注释
        pos++;              // 指针指向下一个 raw token
        skipTrivia();       // 立即吞掉紧随其后的注释（若有）
        return t;
    }

    /**
     * 匹配当前 Token 的词素与指定字符串，若匹配则消费该 token 并前移指针。
     *
     * @param lexeme 待匹配的词素字符串
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
     * 断言当前 Token 的词素与指定值相符，否则抛出 {@link ParseException}。
     * 匹配成功会消费该 token 并前移指针。
     *
     * @param lexeme 期望的词素值
     * @return 匹配成功的 Token
     * @throws ParseException 若词素不符
     */
    public Token expect(String lexeme) {
        Token t = peek();
        if (!t.getLexeme().equals(lexeme)) {
            throw new ParseException(
                    "Expected lexeme '" + lexeme + "' but got '" + t.getLexeme() +
                            "' at " + t.getLine() + ":" + t.getCol()
            );
        }
        return next();
    }

    /**
     * 断言当前 Token 类型为指定类型，否则抛出 {@link ParseException}。
     * 匹配成功会消费该 token 并前移指针。
     *
     * @param type 期望的 Token 类型
     * @return 匹配成功的 Token
     * @throws ParseException 若类型不匹配
     */
    public Token expectType(TokenType type) {
        Token t = peek();
        if (t.getType() != type) {
            throw new ParseException(
                    "Expected token type " + type + " but got " + t.getType() +
                            " ('" + t.getLexeme() + "') at " + t.getLine() + ":" + t.getCol()
            );
        }
        return next();
    }

    /**
     * 判断是否“已经”到达文件末尾（EOF）。
     *
     * @return 若当前位置 Token 为 EOF，则返回 true，否则返回 false
     */
    public boolean isAtEnd() {
        return peek().getType() == TokenType.EOF;
    }

    /**
     * 跳过所有连续的注释（COMMENT）token。
     *
     * <p>
     * 此方法会检查当前指针 <code>pos</code> 所指向的 token，
     * 如果其类型为 <code>TokenType.COMMENT</code>，则直接将指针递增，
     * 直到遇到非 COMMENT 类型或到达 token 列表末尾。
     * </p>
     *
     * <p>
     * 注意：此方法<strong>只会跳过注释</strong>，不会递归或调用任何
     * 会产生递归的方法（如 peek()/next()），以避免堆栈溢出。
     * </p>
     *
     * <p>
     * 使用场景：词法分析产物中允许出现注释 token，语法分析时需要自动跳过它们，
     * 保证 parser 只处理有效语法 token。
     * </p>
     */
    private void skipTrivia() {
        while (pos < tokens.size()
                && tokens.get(pos).getType() == TokenType.COMMENT) {
            pos++; // 直接跳过 COMMENT 类型
        }
    }
}
