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
 * 支持以下两种语法结构：
 * <pre>{@code
 * declare myVar:Integer
 * declare myVar:Integer = 42 + 3
 * }</pre>
 * 解析器能够识别多维数组类型（如 {@code int[]}, {@code string[][]}），并支持可选初始化表达式。
 * <p>
 * 每个声明语句均要求以换行符结尾，语法不合法时会抛出异常。
 * </p>
 */
public class DeclarationStatementParser implements StatementParser {

    /**
     * 解析一条 {@code declare} 语句，生成对应的抽象语法树节点 {@link DeclarationNode}。
     * <p>
     * 支持类型标注和可选初始化表达式。类型部分自动拼接数组维度（如 int[][]）。
     * </p>
     *
     * @param ctx 当前语法解析上下文（包含词法流、错误信息等）
     * @return {@link DeclarationNode} 表示声明语句结构
     * @throws RuntimeException 语法不合法时抛出
     */
    @Override
    public DeclarationNode parse(ParserContext ctx) {
        // 便捷引用词法 token 流
        var tokens = ctx.getTokens();

        // 获取当前 token 的行号、列号和文件名
        int line = tokens.peek().getLine();
        int column = tokens.peek().getCol();
        String file = ctx.getSourceName();

        // 声明语句必须以 "declare" 开头
        tokens.expect("declare");

        // 是否声明为常量
        boolean isConst = tokens.match("const");

        // 获取变量名称（标识符）
        String name = tokens
                .expectType(TokenType.IDENTIFIER)
                .getLexeme();

        // 类型标注的冒号分隔符
        tokens.expect(":");

        // 获取变量类型（类型标识符）
        StringBuilder type = new StringBuilder(
                tokens
                        .expectType(TokenType.TYPE)
                        .getLexeme()
        );

        // 消费多维数组类型后缀 "[]"
        while (tokens.match("[")) {
            tokens.expectType(TokenType.RBRACKET); // 必须配对
            type.append("[]"); // 类型名称拼接 []，如 int[][] 等
        }

        // 可选初始化表达式（=号右侧）
        ExpressionNode init = null;
        if (tokens.match("=")) {
            init = new PrattExpressionParser().parse(ctx);
        }

        // 声明语句必须以换行符结尾
        tokens.expectType(TokenType.NEWLINE);

        // 返回构建好的声明语法树节点
        return new DeclarationNode(name, type.toString(), isConst, init, new NodeContext(line, column, file));
    }
}
