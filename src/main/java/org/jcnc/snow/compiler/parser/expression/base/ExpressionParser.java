package org.jcnc.snow.compiler.parser.expression.base;

import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.context.ParserContext;

/**
 * {@code ExpressionParser} 是用于解析表达式的通用接口。
 * <p>
 * 实现该接口的解析器应根据 {@link ParserContext} 中提供的 Token 流，
 * 构建一个有效的 {@link ExpressionNode} 抽象语法树结构。
 * </p>
 * <p>
 * 不同的实现可以采用不同的解析技术:
 * <ul>
 *     <li>递归下降（Recursive Descent）</li>
 *     <li>Pratt Parser（前缀/中缀优先级驱动）</li>
 *     <li>操作符优先表等其他手段</li>
 * </ul>
 * 通常用于函数体、表达式语句、条件判断等上下文中的子树构建。
 * </p>
 */
public interface ExpressionParser {

    /**
     * 从解析上下文中解析并返回一个表达式节点。
     *
     * @param ctx 当前语法解析上下文，提供 Token 流与辅助状态
     * @return 构建完成的 {@link ExpressionNode} 表达式 AST 子节点
     */
    ExpressionNode parse(ParserContext ctx);
}