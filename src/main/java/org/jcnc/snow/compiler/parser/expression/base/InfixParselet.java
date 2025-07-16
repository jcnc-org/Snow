package org.jcnc.snow.compiler.parser.expression.base;

import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.expression.Precedence;

/**
 * {@code InfixParselet} 表示中缀表达式的解析器接口。
 * <p>
 * 用于构建如 {@code a + b}、{@code x * y}、{@code f(x)} 或 {@code obj.prop} 等结构，
 * 是 Pratt 解析器架构中处理中缀操作的关键组件。
 * </p>
 * <p>
 * 每个中缀解析器负责: 
 * <ul>
 *     <li>根据左侧已解析的表达式，结合当前运算符继续解析右侧部分</li>
 *     <li>提供运算符优先级，用于判断是否继续嵌套解析</li>
 * </ul>
 * </p>
 */
public interface InfixParselet {

    /**
     * 根据已解析的左侧表达式与上下文，解析出完整的中缀表达式节点。
     *
     * @param ctx  当前解析上下文，包含 Token 流状态
     * @param left 当前已解析的左表达式节点
     * @return 组合完成的中缀表达式 AST 节点
     */
    ExpressionNode parse(ParserContext ctx, ExpressionNode left);

    /**
     * 获取当前中缀表达式的解析优先级。
     * <p>
     * 用于决定当前操作符是否绑定左右子表达式，影响解析树结构。
     * </p>
     *
     * @return 表达式优先级枚举值
     */
    Precedence precedence();
}