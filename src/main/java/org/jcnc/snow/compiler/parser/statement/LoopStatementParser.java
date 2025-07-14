package org.jcnc.snow.compiler.parser.statement;

import org.jcnc.snow.compiler.lexer.token.TokenType;
import org.jcnc.snow.compiler.parser.ast.AssignmentNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.LoopNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.context.TokenStream;
import org.jcnc.snow.compiler.parser.expression.PrattExpressionParser;
import org.jcnc.snow.compiler.parser.factory.StatementParserFactory;
import org.jcnc.snow.compiler.parser.utils.FlexibleSectionParser;
import org.jcnc.snow.compiler.parser.utils.ParserUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code LoopStatementParser} 类负责解析自定义结构化的 {@code loop} 语句块。
 * <p>
 * 该语法结构参考了传统的 for-loop，并将其拆解为命名的语义区块：
 * <pre>{@code
 * loop:
 *     init:
 *         declare i: int = 0
 *     cond:
 *         i < 10
 *     step:
 *         i = i + 1
 *     body:
 *         …
 *     end body
 * end loop
 * }</pre>
 *
 * 各区块说明：
 * <ul>
 *   <li>{@code init}：初始化语句，通常为变量声明。</li>
 *   <li>{@code cond}：循环判断条件，必须为布尔或数值表达式。</li>
 *   <li>{@code step}：每轮执行后更新逻辑，通常为赋值语句。</li>
 *   <li>{@code body}：主执行语句块，支持任意多条语句。</li>
 * </ul>
 * 本类依赖 {@link FlexibleSectionParser} 实现各区块的统一处理，确保结构明确、可扩展。
 */
public class LoopStatementParser implements StatementParser {

    /**
     * 解析 {@code loop} 语句块，构建出对应的 {@link LoopNode} 抽象语法树节点。
     * <p>
     * 本方法会按顺序检查各个命名区块（可乱序书写），并分别绑定其对应语义解析器：
     * <ul>
     *   <li>通过 {@link ParserUtils#matchHeader} 匹配区块开头；</li>
     *   <li>通过 {@link FlexibleSectionParser} 派发区块逻辑；</li>
     *   <li>通过 {@link StatementParserFactory} 调用实际语句解析；</li>
     *   <li>最后以 {@code end loop} 表示结构终止。</li>
     * </ul>
     *
     * @param ctx 当前解析上下文。
     * @return {@link LoopNode}，包含初始化、条件、更新与循环体等信息。
     */
    @Override
    public LoopNode parse(ParserContext ctx) {
        TokenStream ts = ctx.getTokens();

        // 获取当前 token 的行号、列号
        int loop_line = ctx.getTokens().peek().getLine();
        int loop_column = ctx.getTokens().peek().getCol();

        String file = ctx.getSourceName();

        // 匹配 loop: 起始语法
        ParserUtils.matchHeader(ts, "loop");

        // 使用数组模拟引用以便在 lambda 中写入（Java 不支持闭包内修改局部变量）
        final StatementNode[] init = new StatementNode[1];
        final ExpressionNode[] cond = new ExpressionNode[1];
        final AssignmentNode[] step = new AssignmentNode[1];
        final List<StatementNode> body = new ArrayList<>();

        // 定义各命名区块的识别与处理逻辑
        Map<String, FlexibleSectionParser.SectionDefinition> sections = new HashMap<>();

        // init 区块：仅支持一条语句，通常为 declare
        sections.put("init", new FlexibleSectionParser.SectionDefinition(
                ts1 -> ts1.peek().getLexeme().equals("init"),
                (ctx1, ts1) -> {
                    ParserUtils.matchHeader(ts1, "init");
                    init[0] = StatementParserFactory.get(ts1.peek().getLexeme()).parse(ctx1);
                    ParserUtils.skipNewlines(ts1);
                }
        ));

        // cond 区块：支持任意可解析为布尔的表达式
        sections.put("cond", new FlexibleSectionParser.SectionDefinition(
                ts1 -> ts1.peek().getLexeme().equals("cond"),
                (ctx1, ts1) -> {
                    ParserUtils.matchHeader(ts1, "cond");
                    cond[0] = new PrattExpressionParser().parse(ctx1);
                    ts1.expectType(TokenType.NEWLINE);
                    ParserUtils.skipNewlines(ts1);
                }
        ));

        // step 区块：目前仅支持单一变量赋值语句
        sections.put("step", new FlexibleSectionParser.SectionDefinition(
                ts1 -> ts1.peek().getLexeme().equals("step"),
                (ctx1, ts1) -> {
                    // 获取当前 token 的行号、列号
                    int line = ctx.getTokens().peek().getLine();
                    int column = ctx.getTokens().peek().getCol();

                    ParserUtils.matchHeader(ts1, "step");
                    String varName = ts1.expectType(TokenType.IDENTIFIER).getLexeme();
                    ts1.expect("=");
                    ExpressionNode expr = new PrattExpressionParser().parse(ctx1);
                    ts1.expectType(TokenType.NEWLINE);
                    step[0] = new AssignmentNode(varName, expr, new NodeContext(line, column, file));
                    ParserUtils.skipNewlines(ts1);
                }
        ));

        // body 区块：支持多条语句，直到遇到 end body
        sections.put("body", new FlexibleSectionParser.SectionDefinition(
                ts1 -> ts1.peek().getLexeme().equals("body"),
                (ctx1, ts1) -> {
                    ParserUtils.matchHeader(ts1, "body");

                    while (!(ts1.peek().getLexeme().equals("end") &&
                            ts1.peek(1).getLexeme().equals("body"))) {
                        String keyword = ts1.peek().getType() == TokenType.KEYWORD
                                ? ts1.peek().getLexeme()
                                : "";
                        body.add(StatementParserFactory.get(keyword).parse(ctx1));
                        ParserUtils.skipNewlines(ts1);
                    }

                    ts1.expect("end");
                    ts1.expect("body");
                    ts1.expectType(TokenType.NEWLINE);
                    ParserUtils.skipNewlines(ts1);
                }
        ));

        // 使用通用区块解析器处理各命名结构块
        FlexibleSectionParser.parse(ctx, ts, sections);

        // 解析结尾的 end loop 标记
        ParserUtils.matchFooter(ts, "loop");

        // 返回构造完成的 LoopNode
        return new LoopNode(init[0], cond[0], step[0], body, new NodeContext(loop_line, loop_column, file));
    }
}
