package org.jcnc.snow.compiler.parser.statement;

import org.jcnc.snow.compiler.lexer.token.TokenType;
import org.jcnc.snow.compiler.parser.ast.DeclarationNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.expression.PrattExpressionParser;

/**
 * {@code DeclarationStatementParser} 负责解析变量声明语句节点。
 * <p>
 * <ul>
 *     <li>支持类型标识符与自定义结构体名</li>
 *     <li>支持多维数组类型，如 <code>int[][]</code></li>
 *     <li>支持带初始值的声明</li>
 * </ul>
 */
public class DeclarationStatementParser implements StatementParser {

    /**
     * 解析变量或常量声明语句。
     *
     * @param ctx 语法分析上下文，提供词法单元流与其他辅助功能
     * @return 解析得到的声明节点 {@link DeclarationNode}
     * @throws org.jcnc.snow.compiler.parser.context.UnexpectedToken 若语法不合法则抛出异常
     */
    @Override
    public DeclarationNode parse(ParserContext ctx) {
        var tokens = ctx.getTokens(); // 获取词法单元流

        // 记录声明语句在源码中的位置信息（行、列、文件名）
        int line = tokens.peek().getLine();
        int column = tokens.peek().getCol();
        String file = ctx.getSourceName();

        // 判断并消费声明关键字 declare 或 const
        boolean isConst = false;
        String first = tokens.peek().getLexeme();
        if ("declare".equals(first)) {
            tokens.next(); // 消费 declare
            // declare 后可选 const，用于声明常量
            if ("const".equals(tokens.peek().getLexeme())) {
                isConst = true;
                tokens.next(); // 消费 const
            }
        } else if ("const".equals(first)) {
            // 支持 const 开头的声明写法
            isConst = true;
            tokens.next(); // 消费 const
        } else {
            // 不符合语法规则，抛出异常
            throw new org.jcnc.snow.compiler.parser.context.UnexpectedToken(
                    "声明应以 'declare' 或 'declare const' 开始，而不是 '" + first + "'",
                    tokens.peek().getLine(), tokens.peek().getCol());
        }

        // 获取变量名（标识符）
        String name = tokens.expectType(TokenType.IDENTIFIER).getLexeme();

        // 检查并消费冒号 “:”
        tokens.expect(":");

        // 解析变量类型（类型标识符或自定义结构体名）
        StringBuilder type = new StringBuilder();
        if (tokens.peek().getType() == TokenType.TYPE || tokens.peek().getType() == TokenType.IDENTIFIER) {
            // 类型可以是基础类型或结构体名
            type.append(tokens.next().getLexeme());
        } else {
            // 类型不是合法的 Token，抛出异常
            var t = tokens.peek();
            throw new org.jcnc.snow.compiler.parser.context.UnexpectedToken(
                    "期望的标记类型为 TYPE 或 IDENTIFIER，但实际得到的是 "
                            + t.getType() + " ('" + t.getLexeme() + "')",
                    t.getLine(), t.getCol()
            );
        }

        // 处理多维数组类型后缀（支持 int[][] 等类型）
        while (tokens.match("[")) {
            // 消费左中括号 '[' 后必须跟右中括号 ']'
            tokens.expectType(TokenType.RBRACKET); // 消费 ']'
            type.append("[]"); // 追加数组后缀
        }

        // 可选的初始化表达式（如 = 10）
        ExpressionNode init = null;
        if (tokens.match("=")) {
            // 使用 Pratt 解析器解析表达式，获得初始化表达式节点
            init = new PrattExpressionParser().parse(ctx);
        }

        // 声明语句必须以换行符 NEWLINE 结尾
        tokens.expectType(TokenType.NEWLINE);

        // 组装声明节点并返回
        return new DeclarationNode(
                name,               // 变量/常量名
                type.toString(),    // 类型字符串
                isConst,            // 是否常量
                init,               // 初始化表达式节点（可为 null）
                new NodeContext(line, column, file) // 源码位置信息
        );
    }
}
