package org.jcnc.snow.compiler.parser.expression.base;

import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.lexer.token.Token;

/**
 * {@code PrefixParselet} 是用于解析前缀表达式的通用接口。
 * <p>
 * 前缀表达式是以某个词法单元（Token）作为起始的表达式结构，
 * 常见类型包括：
 * <ul>
 *     <li>数字字面量（如 {@code 42}）</li>
 *     <li>标识符（如 {@code foo}）</li>
 *     <li>括号包裹的子表达式（如 {@code (a + b)}）</li>
 *     <li>前缀一元运算（如 {@code -x}、{@code !flag}）</li>
 * </ul>
 * </p>
 * <p>
 * 本接口通常用于 Pratt 解析器架构中，负责识别语法的起点。
 * </p>
 */
public interface PrefixParselet {

    /**
     * 解析一个以当前 Token 开头的前缀表达式节点。
     *
     * @param ctx   当前解析上下文，包含 Token 流状态
     * @param token 当前读取到的前缀 Token
     * @return 构建完成的 {@link ExpressionNode} 表达式节点
     */
    ExpressionNode parse(ParserContext ctx, Token token);
}