package org.jcnc.snow.compiler.parser.expression;

import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.parser.ast.BinaryExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.expression.base.InfixParselet;

/**
 * {@code BinaryOperatorParselet} 表示用于解析二元中缀表达式的解析器。
 * <p>
 * 该解析器支持二元运算符表达式（如 {@code a + b}、{@code x * y}），
 * 可配置操作符优先级与结合性（左结合或右结合）。
 * 适用于 Pratt 解析器架构中中缀阶段的语法处理。
 * </p>
 *
 * @param precedence 当前运算符的优先级
 * @param leftAssoc  是否为左结合运算符
 */
public record BinaryOperatorParselet(Precedence precedence, boolean leftAssoc) implements InfixParselet {

    /**
     * 构造一个中缀二元运算符的解析器。
     *
     * @param precedence 运算符的优先级
     * @param leftAssoc  是否左结合（true 表示左结合，false 表示右结合）
     */
    public BinaryOperatorParselet {
    }

    /**
     * 解析当前二元中缀表达式。
     *
     * @param ctx  当前解析上下文
     * @param left 当前已解析的左表达式
     * @return 构建完成的 {@link BinaryExpressionNode} AST 节点
     */
    @Override
    public ExpressionNode parse(ParserContext ctx, ExpressionNode left) {
        // 获取当前 token 的行号、列号和文件名
        int line = ctx.getTokens().peek().getLine();
        int column = ctx.getTokens().peek().getCol();
        String file = ctx.getSourceName();

        Token op = ctx.getTokens().next();
        int prec = precedence.ordinal();

        // 右侧表达式根据结合性确定优先级绑定
        ExpressionNode right = new PrattExpressionParser().parseExpression(ctx, leftAssoc ? Precedence.values()[prec] : Precedence.values()[prec - 1]);

        return new BinaryExpressionNode(left, op.getLexeme(), right, new NodeContext(line, column, file));
    }

    /**
     * 获取当前运算符的优先级。
     *
     * @return 运算符优先级枚举
     */
    @Override
    public Precedence precedence() {
        return precedence;
    }
}
