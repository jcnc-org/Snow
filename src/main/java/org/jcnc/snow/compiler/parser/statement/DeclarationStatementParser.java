package org.jcnc.snow.compiler.parser.statement;

import org.jcnc.snow.compiler.lexer.token.TokenType;
import org.jcnc.snow.compiler.parser.ast.DeclarationNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.expression.PrattExpressionParser;

/**
 * {@code DeclarationStatementParser} 类负责解析变量声明语句，是语句级解析器的一部分。
 * <p>
 * 本解析器支持以下两种形式的声明语法：
 * <pre>{@code
 * declare myVar:Integer
 * declare myVar:Integer = 42 + 3
 * }</pre>
 * 其中：
 * <ul>
 *   <li>{@code myVar} 为变量名（必须为标识符类型）；</li>
 *   <li>{@code Integer} 为类型标注（必须为类型标记）；</li>
 *   <li>可选的初始化表达式由 {@link PrattExpressionParser} 解析；</li>
 *   <li>每条声明语句必须以换行符（{@code NEWLINE}）结束。</li>
 * </ul>
 * 若语法不满足上述结构，将在解析过程中抛出异常。
 */
public class DeclarationStatementParser implements StatementParser {

    /**
     * 解析一条 {@code declare} 声明语句，并返回对应的抽象语法树节点 {@link DeclarationNode}。
     * <p>
     * 解析流程如下：
     * <ol>
     *   <li>匹配关键字 {@code declare}；</li>
     *   <li>读取变量名称（标识符类型）；</li>
     *   <li>读取类型标注（在冒号后，要求为 {@code TYPE} 类型）；</li>
     *   <li>若存在 {@code =}，则继续解析其后的表达式作为初始化值；</li>
     *   <li>最终必须匹配 {@code NEWLINE} 表示语句结束。</li>
     * </ol>
     * 若遇到非法语法结构，将触发异常并中断解析过程。
     *
     * @param ctx 当前语法解析上下文，包含词法流、错误信息等。
     * @return 返回一个 {@link DeclarationNode} 节点，表示解析完成的声明语法结构。
     */
    @Override
    public DeclarationNode parse(ParserContext ctx) {
        // 获取当前 token 的行号、列号和文件名
        int line = ctx.getTokens().peek().getLine();
        int column = ctx.getTokens().peek().getCol();
        String file = ctx.getSourceName();

        // 声明语句必须以 "declare" 开头
        ctx.getTokens().expect("declare");

        // 获取变量名称（标识符）
        String name = ctx.getTokens()
                .expectType(TokenType.IDENTIFIER)
                .getLexeme();

        // 类型标注的冒号分隔符
        ctx.getTokens().expect(":");

        // 获取变量类型（类型标识符）
        String type = ctx.getTokens()
                .expectType(TokenType.TYPE)
                .getLexeme();

        // 可选的初始化表达式，若存在 "="，则解析等号右侧表达式
        ExpressionNode init = null;
        if (ctx.getTokens().match("=")) {
            init = new PrattExpressionParser().parse(ctx);
        }

        // 声明语句必须以换行符结尾
        ctx.getTokens().expectType(TokenType.NEWLINE);

        // 返回构建好的声明语法树节点
        return new DeclarationNode(name, type, init, line, column, file);
    }
}
