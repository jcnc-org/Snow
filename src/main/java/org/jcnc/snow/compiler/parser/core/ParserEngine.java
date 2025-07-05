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
 * <p>驱动顶层解析，并在捕获异常后通过同步机制恢复，防止死循环。</p>
 */
public record ParserEngine(ParserContext ctx) {

    /** 解析整份 TokenStream，返回顶层 AST 节点列表。 */
    public List<Node> parse() {
        List<Node> nodes = new ArrayList<>();
        List<String> errs  = new ArrayList<>();
        TokenStream   ts   = ctx.getTokens();

        // 主循环至 EOF
        while (ts.isAtEnd()) {
            // 跳过空行
            if (ts.peek().getType() == TokenType.NEWLINE) {
                ts.next();
                continue;
            }

            TopLevelParser parser = TopLevelParserFactory.get(ts.peek().getLexeme());
            try {
                nodes.add(parser.parse(ctx));
            } catch (Exception ex) {
                errs.add(ex.getMessage());
                synchronize(ts);                 // 出错后同步恢复
            }
        }

        // 聚合并抛出全部语法错误
        if (!errs.isEmpty()) {
            StringJoiner sj = new StringJoiner("\n - ", "", "");
            errs.forEach(sj::add);
            throw new UnexpectedToken("解析过程中检测到 "
                    + errs.size() + " 处错误:\n - " + sj);
        }
        return nodes;
    }

    /**
     * 同步：跳过当前行或直到遇到 **显式注册** 的顶层关键字。
     * 这样可避免因默认脚本解析器导致指针停滞而进入死循环。
     */
    private void synchronize(TokenStream ts) {
        while (ts.isAtEnd()) {
            if (ts.peek().getType() == TokenType.NEWLINE) {
                ts.next();
                break;
            }
            if (TopLevelParserFactory.isRegistered(ts.peek().getLexeme())) {
                break;                           // 仅在已注册关键字处停下
            }
            ts.next();                          // 继续丢弃 token
        }
        // 清掉后续连续空行
        while (ts.isAtEnd() && ts.peek().getType() == TokenType.NEWLINE) {
            ts.next();
        }
    }
}
