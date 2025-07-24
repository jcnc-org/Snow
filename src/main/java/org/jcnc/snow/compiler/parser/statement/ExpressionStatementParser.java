package org.jcnc.snow.compiler.parser.statement;

import org.jcnc.snow.compiler.lexer.token.TokenType;
import org.jcnc.snow.compiler.parser.ast.AssignmentNode;
import org.jcnc.snow.compiler.parser.ast.ExpressionStatementNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.context.TokenStream;
import org.jcnc.snow.compiler.parser.context.UnexpectedToken;
import org.jcnc.snow.compiler.parser.expression.PrattExpressionParser;

/**
 * {@code ExpressionStatementParser} 用于解析通用表达式语句（赋值或普通表达式）。
 * <p>
 * 支持以下两种语法结构: 
 * <pre>{@code
 * x = 1 + 2        // 赋值语句
 * doSomething()    // 一般表达式语句
 * }</pre>
 * <ul>
 *     <li>以标识符开头且后接 {@code =} 时，解析为 {@link AssignmentNode}。</li>
 *     <li>否则视为普通表达式，解析为 {@link ExpressionStatementNode}。</li>
 *     <li>所有表达式语句必须以换行符（{@code NEWLINE}）结尾。</li>
 * </ul>
 * 若语句起始为关键字或空行，将直接抛出异常，防止非法语法进入表达式解析流程。
 */
public class ExpressionStatementParser implements StatementParser {

    /**
     * 解析单行表达式语句，根据上下文判断其为赋值语句或普通表达式语句。
     *
     * @param ctx 当前解析上下文，提供词法流与环境信息
     * @return {@link AssignmentNode} 或 {@link ExpressionStatementNode} 语法节点
     * @throws UnexpectedToken 若遇到非法起始（关键字、空行等）
     */
    @Override
    public StatementNode parse(ParserContext ctx) {
        TokenStream ts = ctx.getTokens();

        if (ts.peek().getType() == TokenType.NEWLINE || ts.peek().getType() == TokenType.KEYWORD) {
            throw new UnexpectedToken(
                    "无法解析以关键字开头的表达式: " + ts.peek().getLexeme(),
                    ts.peek().getLine(),
                    ts.peek().getCol()
            );
        }

        int line = ts.peek().getLine();
        int column = ts.peek().getCol();
        String file = ctx.getSourceName();

        // 赋值语句: IDENTIFIER = expr
        if (ts.peek().getType() == TokenType.IDENTIFIER && "=".equals(ts.peek(1).getLexeme())) {
            String varName = ts.next().getLexeme();
            ts.expect("=");
            ExpressionNode value = new PrattExpressionParser().parse(ctx);
            ts.expectType(TokenType.NEWLINE);
            return new AssignmentNode(varName, value, new NodeContext(line, column, file));
        }

        // 普通表达式语句
        ExpressionNode expr = new PrattExpressionParser().parse(ctx);
        ts.expectType(TokenType.NEWLINE);
        return new ExpressionStatementNode(expr, new NodeContext(line, column, file));
    }
}
