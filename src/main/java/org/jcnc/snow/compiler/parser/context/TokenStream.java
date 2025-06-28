package org.jcnc.snow.compiler.parser.context;

import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.lexer.token.TokenType;

import java.util.List;

/**
 * {@code TokenStream} 封装了一个 Token 列表并维护当前解析位置，
 * 是语法分析器读取词法单元的核心工具类。
 * <p>
 * 提供前瞻（peek）、消费（next）、匹配（match）、断言（expect）等常用操作，
 * 支持前向查看和异常处理，适用于递归下降解析等常见语法构建策略。
 * </p>
 */
public class TokenStream {

    /** 源 Token 列表 */
    private final List<Token> tokens;

    /** 当前解析位置索引 */
    private int pos = 0;

    /**
     * 使用 Token 列表构造 TokenStream。
     *
     * @param tokens 由词法分析器产生的 Token 集合
     */
    public TokenStream(List<Token> tokens) {
        this.tokens = tokens;
    }

    /**
     * 向前查看指定偏移量处的 Token（不移动位置）。
     *
     * @param offset 相对当前位置的偏移量（0 表示当前）
     * @return 指定位置的 Token；若越界则返回自动构造的 EOF Token
     */
    public Token peek(int offset) {
        int idx = pos + offset;
        if (idx >= tokens.size()) {
            return Token.eof(tokens.size() + 1);
        }
        return tokens.get(idx);
    }

    /**
     * 查看当前位置的 Token，等效于 {@code peek(0)}。
     *
     * @return 当前 Token
     */
    public Token peek() {
        return peek(0);
    }

    /**
     * 消费当前位置的 Token 并返回，位置前移。
     *
     * @return 当前 Token
     */
    public Token next() {
        Token t = peek();
        pos++;
        return t;
    }

    /**
     * 匹配当前 Token 的词素与指定字符串，若匹配则消费。
     *
     * @param lexeme 待匹配词素
     * @return 若成功匹配则返回 true
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
     * 判断是否“已经”到达 EOF。
     *
     * @return 若当前位置 Token 为 EOF，则返回 true，否则 false
     */
    public boolean isAtEnd() {
        return peek().getType() == TokenType.EOF;
    }


}