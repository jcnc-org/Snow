package org.jcnc.snow.compiler.parser.statement;

import org.jcnc.snow.compiler.lexer.token.TokenType;
import org.jcnc.snow.compiler.parser.ast.AssignmentNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.ExpressionStatementNode;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.context.TokenStream;
import org.jcnc.snow.compiler.parser.expression.PrattExpressionParser;

/**
 * {@code ExpressionStatementParser} 负责解析通用表达式语句，包括赋值语句和单一表达式语句。
 * <p>
 * 支持的语法结构如下：
 * <pre>{@code
 * x = 1 + 2        // 赋值语句
 * doSomething()    // 函数调用等普通表达式语句
 * }</pre>
 * <ul>
 *     <li>若以标识符开头，且后接等号 {@code =}，则视为赋值语句，解析为 {@link AssignmentNode}。</li>
 *     <li>否则视为普通表达式，解析为 {@link ExpressionStatementNode}。</li>
 *     <li>所有表达式语句必须以换行符 {@code NEWLINE} 结束。</li>
 * </ul>
 * 不允许以关键字或空行作为表达式的起始，若遇到非法开头，将抛出解析异常。
 */
public class ExpressionStatementParser implements StatementParser {

    /**
     * 解析一个表达式语句，根据上下文决定其为赋值或一般表达式。
     * <p>
     * 具体逻辑如下：
     * <ol>
     *     <li>若当前行为标识符后接等号，则作为赋值处理。</li>
     *     <li>否则解析整个表达式作为单独语句。</li>
     *     <li>所有语句都必须以换行符结束。</li>
     *     <li>若表达式以关键字或空行开头，将立即抛出异常，避免非法解析。</li>
     * </ol>
     *
     * @param ctx 当前解析上下文，提供词法流与状态信息。
     * @return 返回 {@link AssignmentNode} 或 {@link ExpressionStatementNode} 表示的语法节点。
     * @throws IllegalStateException 若表达式起始为关键字或语法非法。
     */
    @Override
    public StatementNode parse(ParserContext ctx) {
        TokenStream ts = ctx.getTokens();

        // 快速检查：若遇空行或关键字开头，不可作为表达式语句
        if (ts.peek().getType() == TokenType.NEWLINE || ts.peek().getType() == TokenType.KEYWORD) {
            throw new IllegalStateException("Cannot parse expression starting with keyword: " + ts.peek().getLexeme());
        }

        // 处理赋值语句：格式为 identifier = expression
        if (ts.peek().getType() == TokenType.IDENTIFIER
                && ts.peek(1).getLexeme().equals("=")) {

            String varName = ts.next().getLexeme(); // 消耗标识符
            ts.expect("=");                         // 消耗等号
            ExpressionNode value = new PrattExpressionParser().parse(ctx); // 解析表达式
            ts.expectType(TokenType.NEWLINE);       // 语句必须以换行符结束
            return new AssignmentNode(varName, value); // 返回赋值节点
        }

        // 处理普通表达式语句，如函数调用、字面量、运算表达式等
        ExpressionNode expr = new PrattExpressionParser().parse(ctx);
        ts.expectType(TokenType.NEWLINE);           // 语句必须以换行符结束
        return new ExpressionStatementNode(expr);   // 返回表达式语句节点
    }

}
