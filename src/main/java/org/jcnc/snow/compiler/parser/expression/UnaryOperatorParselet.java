package org.jcnc.snow.compiler.parser.expression;

import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.parser.ast.UnaryExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.expression.base.PrefixParselet;

/**
 * {@code UnaryOperatorParselet} —— 前缀一元运算符的 Pratt 解析器。
 *
 * <p>当前 parselet 负责解析两种前缀运算:
 * <ul>
 *   <li><b>取负</b>: {@code -x}</li>
 *   <li><b>逻辑非</b>: {@code !x}</li>
 * </ul>
 * <p>
 * 解析过程:
 *
 * <ol>
 *   <li>该 parselet 在外层解析器已消费运算符 {@code token} 后被调用。</li>
 *   <li>以 {@link Precedence#UNARY} 作为 <em>绑定强度</em> 递归解析右侧子表达式，
 *       保证任何更高优先级的表达式（括号、字面量等）优先归属右侧。</li>
 *   <li>最终生成 {@link UnaryExpressionNode} AST 节点，记录运算符与操作数。</li>
 * </ol>
 *
 * <p>此类仅负责<strong>语法结构</strong>的构建:
 *  <ul>
 *    <li>类型正确性在 {@code UnaryExpressionAnalyzer} 中校验；</li>
 *    <li>IR 生成在 {@code ExpressionBuilder.buildUnary} 中完成。</li>
 *  </ul>
 */
public class UnaryOperatorParselet implements PrefixParselet {

    /**
     * 解析前缀一元表达式。
     *
     * @param ctx   当前解析上下文
     * @param token 已被消费的运算符 Token（字面值应为 {@code "-" 或 "!"}）
     * @return 构建出的 {@link UnaryExpressionNode}
     */
    @Override
    public ExpressionNode parse(ParserContext ctx, Token token) {
        // 获取当前 token 的行号、列号和文件名
        int line = ctx.getTokens().peek().getLine();
        int column = ctx.getTokens().peek().getCol();
        String file = ctx.getSourceName();

        /* ------------------------------------------------------------
         * 1. 以 UNARY 优先级递归解析操作数，避免错误结合顺序。
         * ------------------------------------------------------------ */
        ExpressionNode operand =
                new PrattExpressionParser().parseExpression(ctx, Precedence.UNARY);

        /* ------------------------------------------------------------
         * 2. 封装成 AST 节点并返回。
         * ------------------------------------------------------------ */
        return new UnaryExpressionNode(token.getLexeme(), operand, new NodeContext(line, column, file));
    }
}
