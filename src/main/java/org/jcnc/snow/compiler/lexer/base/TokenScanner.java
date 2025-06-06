package org.jcnc.snow.compiler.lexer.base;

import org.jcnc.snow.compiler.lexer.core.LexerContext;
import org.jcnc.snow.compiler.lexer.token.Token;

import java.util.List;

/**
 * {@code TokenScanner} 接口定义了所有词法扫描器的统一行为规范。
 * <p>
 * 编译器前端中的词法分析阶段将源代码字符流解析为语义上有意义的记号（token），
 * 每种类型的记号（如标识符、数字、字符串、符号等）应有对应的 {@code TokenScanner} 实现类。
 * 词法分析器根据当前输入字符判断并分派给能处理该字符的扫描器进行处理。
 * </p>
 * <p>
 * 实现类通常会结合 {@link LexerContext} 提供的流访问与状态接口，
 * 完成一个完整 Token 的提取，并将其添加到结果集中。
 * </p>
 */
public interface TokenScanner {

    /**
     * 判断当前字符是否可以由该扫描器处理。
     * <p>
     * 词法分析器会按顺序查询已注册的 {@code TokenScanner} 实例，
     * 使用该方法决定当前字符是否可由某个扫描器识别与处理。
     * </p>
     *
     * @param c   当前读取的字符
     * @param ctx 当前词法分析上下文，提供字符流和辅助状态
     * @return 若该扫描器可处理当前字符，则返回 {@code true}，否则返回 {@code false}
     */
    boolean canHandle(char c, LexerContext ctx);

    /**
     * 处理以当前字符为起始的 token，并将扫描结果添加至 tokens 列表中。
     * <p>
     * 扫描器需消费一定数量的字符，构建合法的 {@link Token} 实例，
     * 并调用 {@code tokens.add(...)} 添加至结果集中。
     * </p>
     *
     * @param ctx    当前词法上下文
     * @param tokens 存储扫描结果的 token 列表
     */
    void handle(LexerContext ctx, List<Token> tokens);
}