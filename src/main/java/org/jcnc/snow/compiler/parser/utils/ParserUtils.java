package org.jcnc.snow.compiler.parser.utils;

import org.jcnc.snow.compiler.lexer.token.TokenType;
import org.jcnc.snow.compiler.parser.context.TokenStream;

/**
 * {@code ParserUtils} 是语法结构解析过程中的通用辅助工具类。
 * <p>
 * 提供一系列静态方法用于标准语法结构（如结构头、结构尾）的匹配校验，以及常用的容错处理操作。
 * 这些方法可在函数定义、模块定义、循环、条件语句等语法块中复用，有效减少冗余代码，提高解析器稳定性。
 *
 * <p>主要功能包括:
 * <ul>
 *   <li>匹配结构性语法起始标记（如 {@code loop:}、{@code function:}）</li>
 *   <li>匹配结构性语法结尾标记（如 {@code end loop}、{@code end function}）</li>
 *   <li>跳过多余换行符，增强语法容错性</li>
 * </ul>
 */
public class ParserUtils {

    /**
     * 匹配结构语法的标准起始格式 {@code keyword:}，并跳过其后的换行符。
     * <p>
     * 该方法适用于需要标识结构起点的语法元素，如 {@code loop:}、{@code function:} 等。
     * 若格式不匹配，将抛出语法异常。
     *
     * @param ts      当前的 token 流
     * @param keyword 结构起始关键字（如 "loop", "function", "init" 等）
     */
    public static void matchHeader(TokenStream ts, String keyword) {
        ts.expect(keyword);                  // 匹配关键字
        ts.expect(":");                      // 匹配冒号
        ts.expectType(TokenType.NEWLINE);    // 匹配行尾换行
        skipNewlines(ts);                    // 跳过多余空行
    }

    /**
     * 匹配结构语法的标准结尾格式 {@code end keyword}。
     * <p>
     * 该方法用于验证结构块的结束，例如 {@code end loop}、{@code end if} 等。
     * 若格式不正确，将抛出异常。
     *
     * @param ts      当前的 token 流
     * @param keyword 对应的结构关键字（必须与开始标记一致）
     */
    public static void matchFooter(TokenStream ts, String keyword) {
        ts.expect("end");                    // 匹配 'end'
        ts.expect(keyword);                  // 匹配结构名
        ts.expectType(TokenType.NEWLINE);    // 匹配行尾
    }

    /**
     * 跳过连续的换行符（{@code NEWLINE}）。
     * <p>
     * 通常用于解析器之间的过渡阶段，以消除格式干扰，提升容错性。
     *
     * @param ts 当前的 token 流
     */
    public static void skipNewlines(TokenStream ts) {
        while (ts.peek().getType() == TokenType.NEWLINE) {
            ts.next(); // 连续消费换行符
        }
    }
}
