package org.jcnc.snow.compiler.parser.core;

import org.jcnc.snow.compiler.lexer.token.TokenType;
import org.jcnc.snow.compiler.parser.ast.base.Node;
import org.jcnc.snow.compiler.parser.base.TopLevelParser;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.context.TokenStream;
import org.jcnc.snow.compiler.parser.context.UnexpectedToken;
import org.jcnc.snow.compiler.parser.factory.TopLevelParserFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * 语法解析引擎（ParserEngine）。
 * <p>
 * 负责驱动 Snow 源码的顶层语法结构解析，将源码 TokenStream
 * 递交给各类 TopLevelParser，并收集语法树节点与异常。
 * 支持容错解析，能够批量报告所有语法错误，并提供同步恢复功能。
 * </p>
 *
 * <p>
 * 典型用法：
 * <pre>
 *     ParserEngine engine = new ParserEngine(context);
 *     List&lt;Node&gt; ast = engine.parse();
 * </pre>
 * </p>
 *
 * @param ctx 解析器上下文，负责持有 TokenStream 及所有全局状态。
 */
public record ParserEngine(ParserContext ctx) {

    /**
     * 解析输入 TokenStream，生成语法树节点列表。
     *
     * <p>
     * 调用各类顶级语句解析器（如 module, func, import），
     * 遇到错误时会自动跳过到下一行或已知结构关键字，继续后续分析，
     * 最终汇总所有错误。如果解析出现错误，将以
     * {@link UnexpectedToken} 抛出所有语法错误信息。
     * </p>
     *
     * @return AST 节点列表，每个节点对应一个顶层语法结构
     * @throws UnexpectedToken 如果解析期间发现语法错误
     */
    public List<Node> parse() {
        List<Node> nodes = new ArrayList<>();
        List<String> errs = new ArrayList<>();
        TokenStream ts = ctx.getTokens();

        // 主循环：直到全部 token 处理完毕
        while (ts.isAtEnd()) {
            // 跳过所有空行
            if (ts.peek().getType() == TokenType.NEWLINE) {
                ts.next();
                continue;
            }

            TopLevelParser parser = TopLevelParserFactory.get(ts.peek().getLexeme());

            try {
                nodes.add(parser.parse(ctx));
            } catch (Exception ex) {
                errs.add(ex.getMessage());
                synchronize(ts);         // 错误恢复：同步到下一个语句
            }
        }

        // 批量报告所有解析错误
        if (!errs.isEmpty()) {
            StringJoiner sj = new StringJoiner("\n - ", "", "");
            errs.forEach(sj::add);
            throw new UnexpectedToken("解析过程中检测到 "
                    + errs.size() + " 处错误:\n - " + sj);
        }
        return nodes;
    }

    /**
     * 错误同步机制：跳过当前 TokenStream，直到遇到下一行
     * 或下一个可识别的顶级结构关键字，以保证后续解析不会被卡住。
     * <p>
     * 同时会跳过连续空行。
     * </p>
     *
     * @param ts 当前 TokenStream
     */
    private void synchronize(TokenStream ts) {
        // 跳到下一行或下一个顶层结构关键字
        while (ts.isAtEnd()) {
            if (ts.peek().getType() == TokenType.NEWLINE) {
                ts.next();
                break;
            }
            if (TopLevelParserFactory.get(ts.peek().getLexeme()) != null) {
                break;
            }
            ts.next();
        }
        // 吃掉后续所有空行
        while (ts.isAtEnd() && ts.peek().getType() == TokenType.NEWLINE) {
            ts.next();
        }
    }
}
