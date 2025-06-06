package org.jcnc.snow.compiler.parser.core;

import org.jcnc.snow.compiler.lexer.token.TokenType;
import org.jcnc.snow.compiler.parser.base.TopLevelParser;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.context.TokenStream;
import org.jcnc.snow.compiler.parser.factory.TopLevelParserFactory;
import org.jcnc.snow.compiler.parser.ast.base.Node;

import java.util.ArrayList;
import java.util.List;

public record ParserEngine(ParserContext ctx) {

    public List<Node> parse() {
        List<Node> nodes = new ArrayList<>();
        List<String> errs = new ArrayList<>();
        TokenStream ts = ctx.getTokens();

        while (ts.isAtEnd()) {          // ← 取反
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
                synchronize(ts);         // 错误恢复
            }
        }

        if (!errs.isEmpty()) {
            throw new IllegalStateException("解析过程中检测到 "
                    + errs.size() + " 处错误:\n - "
                    + String.join("\n - ", errs));
        }
        return nodes;
    }

    /**
     * 错误同步：跳到下一行或下一个已注册顶层关键字
     */
    private void synchronize(TokenStream ts) {
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
        // 连续空行全部吃掉
        while (ts.isAtEnd() && ts.peek().getType() == TokenType.NEWLINE) {
            ts.next();
        }
    }
}
