package org.jcnc.snow.compiler.parser.expression;

import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.lexer.token.TokenType;
import org.jcnc.snow.compiler.parser.ast.NewExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.context.TokenStream;
import org.jcnc.snow.compiler.parser.context.UnexpectedToken;
import org.jcnc.snow.compiler.parser.expression.base.PrefixParselet;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code NewObjectParselet}
 * <p>
 * 解析对象创建表达式 <code>new TypeName(arg1, arg2, ...)</code> 的前缀解析器实现。
 * <ul>
 *   <li>用于 Pratt 解析框架，实现 new 关键字前缀的自定义解析逻辑。</li>
 *   <li>支持类型名为内置类型（TokenType.TYPE）或用户结构体名（TokenType.IDENTIFIER）。</li>
 *   <li>支持可变参数列表，参数为任意表达式。</li>
 * </ul>
 * </p>
 */
public class NewObjectParselet implements PrefixParselet {

    /**
     * 解析 new 对象创建表达式，并生成 AST 新建节点。
     *
     * @param ctx   语法分析上下文，包含 TokenStream 及源文件名等信息
     * @param token 当前读取到的 new 关键字 Token
     * @return 表达式节点（NewExpressionNode）
     *
     * <p>
     * 解析流程：
     * <ol>
     *   <li>读取类型名（支持内建类型或结构体名）；</li>
     *   <li>读取参数列表，参数为表达式，用逗号分隔；</li>
     *   <li>封装为 NewExpressionNode，记录源码位置信息。</li>
     * </ol>
     * 出错时会抛出 UnexpectedToken 异常。
     * </p>
     */
    @Override
    public ExpressionNode parse(ParserContext ctx, Token token) {
        TokenStream ts = ctx.getTokens();

        // ==========================
        // 1) 解析类型名
        // ==========================
        // 类型名只能为内建类型（TYPE）或用户结构体名（IDENTIFIER）
        if (ts.peek().getType() != TokenType.TYPE && ts.peek().getType() != TokenType.IDENTIFIER) {
            var t = ts.peek();
            throw new UnexpectedToken(
                    "期望的标记类型为 TYPE 或 IDENTIFIER，但实际得到的是 " +
                            t.getType() + " ('" + t.getLexeme() + "')",
                    t.getLine(), t.getCol()
            );
        }
        String typeName = ts.next().getLexeme();

        // ==========================
        // 2) 解析构造参数列表
        // ==========================
        ts.expect("("); // 必须有左括号

        List<ExpressionNode> args = new ArrayList<>();
        if (!ts.match(")")) { // 非空参数列表
            // 连续处理逗号分隔的多个参数
            do {
                args.add(new PrattExpressionParser().parse(ctx));
            } while (ts.match(","));
            ts.expect(")"); // 结尾必须为右括号
        }

        // ==========================
        // 3) 封装为 AST 节点并返回
        // ==========================
        NodeContext nc = new NodeContext(token.getLine(), token.getCol(), ctx.getSourceName());
        return new NewExpressionNode(typeName, args, nc);
    }
}
