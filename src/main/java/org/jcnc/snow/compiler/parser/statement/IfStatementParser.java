package org.jcnc.snow.compiler.parser.statement;

import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.lexer.token.TokenType;
import org.jcnc.snow.compiler.parser.ast.IfNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.expression.PrattExpressionParser;
import org.jcnc.snow.compiler.parser.factory.StatementParserFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code IfStatementParser} 类负责解析 if 条件语句，是语句级解析器中的条件分支处理器。
 * <p>
 * 本解析器支持以下结构的条件语法:
 * <pre>{@code
 * if <condition> then
 *     <then-statements>
 * [else
 *     <else-statements>]
 * end if
 * }</pre>
 * 其中:
 * <ul>
 *     <li>{@code <condition>} 为任意可解析的布尔或数值表达式，使用 {@link PrattExpressionParser} 解析；</li>
 *     <li>{@code <then-statements>} 与 {@code <else-statements>} 可包含多条语句，自动跳过空行；</li>
 *     <li>{@code else} 分支为可选，若存在，必须紧跟换行与语句；</li>
 *     <li>{@code end if} 为终止标识，表示整个 if 语句块的结束。</li>
 * </ul>
 * 所有语句的实际解析由 {@link StatementParserFactory} 根据关键词动态分派处理。
 */
public class IfStatementParser implements StatementParser {

    /**
     * 解析一条完整的 if 条件语句，返回语法树中对应的 {@link IfNode} 节点。
     * <p>
     * 本方法支持 then 分支和可选的 else 分支，并确保以 {@code end if} 正确结尾。
     * 在解析过程中自动跳过空行；遇到未知关键字或不符合预期的 token 时会抛出异常。
     *
     * @param ctx 当前的语法解析上下文，包含 token 流和语义环境。
     * @return 构造完成的 {@link IfNode}，包含条件表达式、then 分支和 else 分支语句列表。
     * @throws IllegalStateException 若语法结构不完整或存在非法 token。
     */
    @Override
    public IfNode parse(ParserContext ctx) {
        var ts = ctx.getTokens(); // 获取 token 流引用

        // 获取当前 token 的行号、列号和文件名
        int line = ctx.getTokens().peek().getLine();
        int column = ctx.getTokens().peek().getCol();
        String file = ctx.getSourceName();

        // 消耗起始关键字 "if"
        ts.expect("if");

        // 使用 Pratt 算法解析 if 条件表达式
        var condition = new PrattExpressionParser().parse(ctx);

        // 条件表达式后必须紧跟 "then" 和换行
        ts.expect("then");
        ts.expectType(TokenType.NEWLINE);

        // 初始化 then 和 else 分支语句列表
        List<StatementNode> thenBranch = new ArrayList<>();
        List<StatementNode> elseBranch = new ArrayList<>();

        // -------------------------
        // 解析 THEN 分支语句块
        // -------------------------
        while (true) {
            Token peek = ts.peek();

            // 跳过空行
            if (peek.getType() == TokenType.NEWLINE) {
                ts.next();
                continue;
            }

            // 遇到 else 或 end 表示 then 分支结束
            if (peek.getType() == TokenType.KEYWORD &&
                    (peek.getLexeme().equals("else") || peek.getLexeme().equals("end"))) {
                break;
            }

            // 获取当前语句的关键字，调用工厂获取对应解析器
            String keyword = peek.getType() == TokenType.KEYWORD ? peek.getLexeme() : "";
            StatementNode stmt = StatementParserFactory.get(keyword).parse(ctx);
            thenBranch.add(stmt);
        }

        // -------------------------
        // 解析 ELSE 分支语句块（可选）
        // -------------------------
        if (ts.peek().getLexeme().equals("else")) {
            ts.next(); // 消耗 "else"
            ts.expectType(TokenType.NEWLINE); // 消耗换行符

            while (true) {
                Token peek = ts.peek();

                // 跳过空行
                if (peek.getType() == TokenType.NEWLINE) {
                    ts.next();
                    continue;
                }

                // "end" 表示 else 分支结束
                if (peek.getType() == TokenType.KEYWORD && peek.getLexeme().equals("end")) {
                    break;
                }

                String keyword = peek.getType() == TokenType.KEYWORD ? peek.getLexeme() : "";
                StatementNode stmt = StatementParserFactory.get(keyword).parse(ctx);
                elseBranch.add(stmt);
            }
        }

        // -------------------------
        // 统一结束处理: end if
        // -------------------------
        ts.expect("end");
        ts.expect("if");
        ts.expectType(TokenType.NEWLINE);

        // 构建并返回 IfNode，包含条件、then 分支和 else 分支
        return new IfNode(condition, thenBranch, elseBranch, new NodeContext(line, column, file));
    }
}
