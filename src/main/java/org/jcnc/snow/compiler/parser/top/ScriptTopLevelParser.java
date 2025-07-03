package org.jcnc.snow.compiler.parser.top;

import org.jcnc.snow.compiler.parser.base.TopLevelParser;
import org.jcnc.snow.compiler.parser.ast.base.Node;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.factory.StatementParserFactory;
import org.jcnc.snow.compiler.parser.statement.StatementParser;

/**
 * {@code ScriptTopLevelParser} 允许在无 module 包裹的情况下
 * 直接解析单条顶层语句（脚本模式）。
 * <p>
 * 解析得到的 {@link StatementNode} 将在 IR 阶段被封装成 _start 函数。
 */
public class ScriptTopLevelParser implements TopLevelParser {

    @Override
    public Node parse(ParserContext ctx) {
        String first = ctx.getTokens().peek().getLexeme();
        StatementParser sp = StatementParserFactory.get(first);
        return sp.parse(ctx);
    }
}
