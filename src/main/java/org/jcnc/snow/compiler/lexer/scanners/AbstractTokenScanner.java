package org.jcnc.snow.compiler.lexer.scanners;

import org.jcnc.snow.compiler.lexer.core.LexerContext;
import org.jcnc.snow.compiler.lexer.base.TokenScanner;
import org.jcnc.snow.compiler.lexer.token.Token;

import java.util.List;
import java.util.function.Predicate;

/**
 * {@code AbstractTokenScanner} 是 {@link TokenScanner} 的抽象实现，
 * 封装了常用的扫描行为与模板逻辑，简化子类的实现负担。
 * <p>
 * 子类只需实现 {@link #scanToken(LexerContext, int, int)} 方法，
 * 专注于处理具体的 Token 构造逻辑，
 * 而位置信息提取、Token 添加等通用操作由本类统一完成。
 * </p>
 */
public abstract class AbstractTokenScanner implements TokenScanner {

    /**
     * 处理当前字符起始的 Token，附带行列信息并加入 Token 列表。
     *
     * @param ctx    当前词法分析上下文
     * @param tokens 存储扫描结果的 Token 列表
     */
    @Override
    public void handle(LexerContext ctx, List<Token> tokens) {
        int line = ctx.getLine();
        int col = ctx.getCol();
        Token token = scanToken(ctx, line, col);
        if (token != null) {
            tokens.add(token);
        }
    }

    /**
     * 抽象方法：由子类实现具体的扫描逻辑。
     * <p>
     * 实现应消费一定字符并根据规则构造 Token。
     * 若无需生成 Token，可返回 null。
     * </p>
     *
     * @param ctx  当前扫描上下文
     * @param line 当前行号
     * @param col  当前列号
     * @return 构造的 Token 或 null
     */
    protected abstract Token scanToken(LexerContext ctx, int line, int col);

    /**
     * 工具方法：连续读取字符直到遇到不满足指定条件的字符。
     *
     * @param ctx       当前词法上下文
     * @param predicate 字符匹配条件
     * @return 满足条件的字符组成的字符串
     */
    protected String readWhile(LexerContext ctx, Predicate<Character> predicate) {
        StringBuilder sb = new StringBuilder();
        while (!ctx.isAtEnd() && predicate.test(ctx.peek())) {
            sb.append(ctx.advance());
        }
        return sb.toString();
    }
}