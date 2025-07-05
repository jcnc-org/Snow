package org.jcnc.snow.compiler.parser.core;

import org.jcnc.snow.compiler.lexer.token.TokenType;
import org.jcnc.snow.compiler.parser.ast.base.Node;
import org.jcnc.snow.compiler.parser.base.TopLevelParser;
import org.jcnc.snow.compiler.parser.context.*;
import org.jcnc.snow.compiler.parser.factory.TopLevelParserFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * 语法解析引擎（ParserEngine）。
 * <p>
 * 负责驱动顶层语法解析，并统一处理、收集所有语法异常，防止死循环，确保整体解析流程的健壮性与鲁棒性。
 * 支持基于同步点的错误恢复，适用于命令式和脚本式语法环境。
 * </p>
 *
 * <p>
 * 本引擎以异常收集为核心设计，所有捕获到的 {@link ParseException} 会被聚合，在分析结束后一次性统一抛出。
 * 同时，在解析出错时会通过同步（synchronize）机制，跳过错误片段以恢复到有效解析点，避免因指针停滞导致的死循环。
 * </p>
 */
public record ParserEngine(ParserContext ctx) {

    /**
     * 解析整个 TokenStream，返回顶层 AST 节点列表。
     * <p>
     * 过程中如遇语法异常，均会被收集并在最后聚合抛出，避免单点失败导致整个解析中断。
     * </p>
     *
     * @return 解析所得的顶层 AST 节点列表
     * @throws UnexpectedToken 当存在语法错误时，统一抛出聚合异常
     */
    public List<Node> parse() {
        List<Node> nodes = new ArrayList<>();
        List<ParseError> errs = new ArrayList<>();

        TokenStream ts = ctx.getTokens();
        String file = ctx.getSourceName();

        // 主循环至 EOF
        while (!ts.isAtEnd()) {
            // 跳过空行
            if (ts.peek().getType() == TokenType.NEWLINE) {
                ts.next();
                continue;
            }

            TopLevelParser parser = TopLevelParserFactory.get(ts.peek().getLexeme());
            try {
                nodes.add(parser.parse(ctx));
            } catch (ParseException ex) {
                // 收集错误并尝试同步
                errs.add(new ParseError(file, ex.getLine(), ex.getColumn(), ex.getReason()));
                synchronize(ts);
            }
        }

        /* ───── 统一抛出聚合异常 ───── */
        if (!errs.isEmpty()) {
            StringJoiner sj = new StringJoiner("\n - ", "", "");
            errs.forEach(e -> sj.add(e.toString()));

            String msg = "解析过程中检测到 " + errs.size() + " 处错误:\n - " + sj;
            throw new UnexpectedToken(msg, 0, 0);
        }
        return nodes;
    }

    /**
     * 同步：跳过当前行或直到遇到显式注册的顶层关键字。
     * <p>
     * 该机制用于语法出错后恢复到下一个可能的有效解析点，防止指针停滞导致死循环或重复抛错。
     * 同步过程中会优先跳过本行所有未识别 token，并在遇到换行或注册关键字时停止，随后跳过连续空行。
     * </p>
     *
     * @param ts 词法 token 流
     */
    private void synchronize(TokenStream ts) {
        while (!ts.isAtEnd()) {
            if (ts.peek().getType() == TokenType.NEWLINE) {
                ts.next();
                break;
            }
            if (TopLevelParserFactory.isRegistered(ts.peek().getLexeme())) {
                break; // 仅在已注册关键字处停下
            }
            ts.next(); // 继续丢弃 token
        }
        // 清理后续连续空行
        while (!ts.isAtEnd() && ts.peek().getType() == TokenType.NEWLINE) {
            ts.next();
        }
    }
}
