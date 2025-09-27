package org.jcnc.snow.compiler.parser.function;

import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.lexer.token.TokenType;
import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.parser.ast.ParameterNode;
import org.jcnc.snow.compiler.parser.ast.ReturnNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;
import org.jcnc.snow.compiler.parser.base.TopLevelParser;
import org.jcnc.snow.compiler.parser.context.ParseException;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.context.TokenStream;
import org.jcnc.snow.compiler.parser.context.UnexpectedToken;
import org.jcnc.snow.compiler.parser.factory.StatementParserFactory;
import org.jcnc.snow.compiler.parser.utils.FlexibleSectionParser;
import org.jcnc.snow.compiler.parser.utils.FlexibleSectionParser.SectionDefinition;

import java.util.*;

/**
 * {@code FunctionParser} 实现顶层函数定义的语法解析，将源代码函数块转换为 AST {@link FunctionNode}。
 *
 * <p>
 * 支持分区解析（params, returns, body），自动跳过注释与空行，并检测参数重定义。
 * 解析异常时会抛出 {@link ParseException} 或 {@link UnexpectedToken}，保证解析流程健壮。
 * </p>
 *
 * <p>
 * 支持的函数声明结构示例：
 * <pre>
 * function: foo
 * params:
 *   declare x: int
 *   declare y: float[]
 * returns: int
 * body:
 *   ...
 * end body
 * end function
 * </pre>
 * </p>
 */
public class FunctionParser implements TopLevelParser {

    /**
     * 解析函数定义并返回 AST {@link FunctionNode}。
     *
     * @param ctx 当前解析上下文
     * @return 构建完成的 FunctionNode
     * @throws ParseException  语法错误（如参数重定义）
     * @throws UnexpectedToken 语法不符
     */
    @Override
    public FunctionNode parse(ParserContext ctx) {
        TokenStream ts = ctx.getTokens();

        int line = ts.peek().getLine();
        int column = ts.peek().getCol();
        String file = ctx.getSourceName();

        parseFunctionHeader(ts);
        String functionName = parseFunctionName(ts);

        List<ParameterNode> parameters = new ArrayList<>();
        String[] returnType = new String[1];
        List<StatementNode> body = new ArrayList<>();

        Map<String, SectionDefinition> sections = getSectionDefinitions(parameters, returnType, body);

        FlexibleSectionParser.parse(ctx, ts, sections);

        if (body.isEmpty() && "void".equals(returnType[0])) {
            body.add(new ReturnNode(null, new NodeContext(line, column, file)));
        }

        Set<String> seen = new HashSet<>();
        for (ParameterNode node : parameters) {
            if (!seen.add(node.name())) {
                throw new ParseException("参数 `" + node.name() + "` 重定义", node.context().line(), node.context().column());
            }
        }

        parseFunctionFooter(ts);

        return new FunctionNode(functionName, parameters, returnType[0], body, new NodeContext(line, column, file));
    }

    /**
     * 定义函数分区（params、returns、body）及其解析逻辑。
     *
     * @param params     参数节点收集器
     * @param returnType 返回类型（数组，仅用第 0 位）
     * @param body       函数体语句节点收集器
     * @return 区块定义映射
     */
    private Map<String, SectionDefinition> getSectionDefinitions(List<ParameterNode> params, String[] returnType, List<StatementNode> body) {
        Map<String, SectionDefinition> map = new HashMap<>();
        map.put("params", new SectionDefinition(
                (TokenStream stream) -> stream.peek().getLexeme().equals("params"),
                (ParserContext context, TokenStream stream) -> params.addAll(parseParameters(context))
        ));
        map.put("returns", new SectionDefinition(
                (TokenStream stream) -> stream.peek().getLexeme().equals("returns"),
                (ParserContext context, TokenStream stream) -> returnType[0] = parseReturnType(stream)
        ));
        map.put("body", new SectionDefinition(
                (TokenStream stream) -> stream.peek().getLexeme().equals("body"),
                (ParserContext context, TokenStream stream) -> body.addAll(parseFunctionBody(context, stream))
        ));
        return map;
    }

    /**
     * 解析函数头（function:）。
     *
     * @param ts token 流
     */
    private void parseFunctionHeader(TokenStream ts) {
        ts.expect("function");
        ts.expect(":");
        skipComments(ts);
        skipNewlines(ts);
    }

    /**
     * 解析函数名（IDENTIFIER）。
     *
     * @param ts token 流
     * @return 函数名称
     */
    private String parseFunctionName(TokenStream ts) {
        String name = ts.expectType(TokenType.IDENTIFIER).getLexeme();
        ts.expectType(TokenType.NEWLINE);
        return name;
    }

    /**
     * 解析函数结束标记（end function）。
     *
     * @param ts token 流
     */
    private void parseFunctionFooter(TokenStream ts) {
        ts.expect("end");
        ts.expect("function");
    }

    /**
     * 解析参数区块。
     *
     * @param ctx 解析上下文
     * @return 参数节点列表
     */
    private List<ParameterNode> parseParameters(ParserContext ctx) {
        TokenStream ts = ctx.getTokens();
        ts.expect("params");
        ts.expect(":");
        skipComments(ts);
        ts.expectType(TokenType.NEWLINE);
        skipNewlines(ts);

        List<ParameterNode> list = new ArrayList<>();
        while (true) {
            skipComments(ts);
            if (ts.peek().getType() == TokenType.NEWLINE) {
                ts.next();
                continue;
            }
            String lex = ts.peek().getLexeme();
            if (lex.equals("returns") || lex.equals("body") || lex.equals("end")) break;

            int line = ts.peek().getLine();
            int column = ts.peek().getCol();
            String file = ctx.getSourceName();

            ts.expect("declare");
            String pname = ts.expectType(TokenType.IDENTIFIER).getLexeme();
            ts.expect(":");
            String ptype = parseTypeWithArray(ts);

            skipComments(ts);
            ts.expectType(TokenType.NEWLINE);
            list.add(new ParameterNode(pname, ptype, new NodeContext(line, column, file)));
        }
        return list;
    }

    /**
     * 解析返回类型区块。
     *
     * @param ts token 流
     * @return 返回类型字符串
     */
    private String parseReturnType(TokenStream ts) {
        ts.expect("returns");
        ts.expect(":");
        skipComments(ts);

        String rtype = parseTypeWithArray(ts);

        skipComments(ts);
        ts.expectType(TokenType.NEWLINE);
        skipNewlines(ts);
        return rtype;
    }

    /**
     * 解析类型名与数组修饰符（如 int[]）。
     *
     * @param ts token 流
     * @return 类型名字符串
     */
    private String parseTypeWithArray(TokenStream ts) {
        Token typeToken;
        if (ts.peek().getType() == TokenType.TYPE || ts.peek().getType() == TokenType.IDENTIFIER) {
            typeToken = ts.next();
        } else {
            var t = ts.peek();
            throw new UnexpectedToken("期望 TYPE 或 IDENTIFIER，但得到 " + t.getType() + " ('" + t.getLexeme() + "')", t.getLine(), t.getCol());
        }
        StringBuilder typeName = new StringBuilder(typeToken.getLexeme());
        while (ts.peek().getType() == TokenType.LBRACKET) {
            ts.next(); // [
            if (ts.peek().getType() != TokenType.RBRACKET) {
                var t = ts.peek();
                throw new UnexpectedToken("数组类型应为 []，但遇到 " + t.getType() + " ('" + t.getLexeme() + "')", t.getLine(), t.getCol());
            }
            ts.next(); // ]
            typeName.append("[]");
        }
        return typeName.toString();
    }

    /**
     * 解析函数体区块，直到 end body。
     *
     * @param ctx 解析上下文
     * @param ts  token 流
     * @return 语句节点列表
     */
    private List<StatementNode> parseFunctionBody(ParserContext ctx, TokenStream ts) {
        ts.expect("body");
        ts.expect(":");
        skipComments(ts);
        ts.expectType(TokenType.NEWLINE);
        skipNewlines(ts);

        List<StatementNode> stmts = new ArrayList<>();
        while (true) {
            skipComments(ts);
            if (ts.peek().getType() == TokenType.NEWLINE) {
                ts.next();
                continue;
            }
            if ("end".equals(ts.peek().getLexeme())) break;
            stmts.add(StatementParserFactory.get(ts.peek().getLexeme()).parse(ctx));
        }
        ts.expect("end");
        ts.expect("body");
        ts.expectType(TokenType.NEWLINE);
        skipNewlines(ts);
        return stmts;
    }

    /**
     * 跳过连续注释 token。
     *
     * @param ts token 流
     */
    private void skipComments(TokenStream ts) {
        while (ts.peek().getType() == TokenType.COMMENT) ts.next();
    }

    /**
     * 跳过连续空行 token。
     *
     * @param ts token 流
     */
    private void skipNewlines(TokenStream ts) {
        while (ts.peek().getType() == TokenType.NEWLINE) ts.next();
    }
}
