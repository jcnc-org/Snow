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
 * {@code FunctionParser} 是顶层函数定义的语法解析器，
 * 实现 {@link TopLevelParser} 接口，用于将源代码中的函数块解析为抽象语法树（AST）中的 {@link FunctionNode}。
 *
 * <p>
 * 使用 {@link FlexibleSectionParser} 机制，按照语义区块结构对函数进行模块化解析，支持以下部分:
 * <ul>
 *   <li>函数头（关键字 {@code function:} 与函数名）</li>
 *   <li>参数列表（params 区块）</li>
 *   <li>返回类型（returns 区块）</li>
 *   <li>函数体（body 区块）</li>
 *   <li>函数结束（关键字 {@code end function}）</li>
 * </ul>
 *
 * <p>
 * 各区块允许包含注释（类型 {@code COMMENT}）与空行（类型 {@code NEWLINE}），解析器将自动跳过无效 token 保持语法连续性。
 * </p>
 *
 * <p>
 * 最终将函数结构封装为 {@link FunctionNode} 并返回，供后续编译阶段使用。
 * </p>
 */
public class FunctionParser implements TopLevelParser {

    /**
     * 顶层语法解析入口。
     *
     * <p>
     * 解析并生成函数的 AST。
     * 该方法从源代码中获取 {@link TokenStream}，并按照函数定义的不同区块解析，最终生成 {@link FunctionNode}。
     * </p>
     *
     * @param ctx 当前解析上下文，包含 {@link TokenStream} 和符号表等信息
     * @return 构建完成的 {@link FunctionNode} 对象
     */
    @Override
    public FunctionNode parse(ParserContext ctx) {
        TokenStream ts = ctx.getTokens();

        int line = ts.peek().getLine();
        int column = ts.peek().getCol();
        String file = ctx.getSourceName();

        // 解析函数头 (function:)
        parseFunctionHeader(ts);

        // 函数名
        String functionName = parseFunctionName(ts);

        List<ParameterNode> parameters = new ArrayList<>();
        String[] returnType = new String[1];  // 使用数组模拟可变引用
        List<StatementNode> body = new ArrayList<>();

        // 定义函数可选区块规则 (params, returns, body)
        Map<String, SectionDefinition> sections = getSectionDefinitions(parameters, returnType, body);

        // 解析这些区块
        FlexibleSectionParser.parse(ctx, ts, sections);

        // 如果函数体为空且返回 void，补充一个空 return
        if (body.isEmpty() && "void".equals(returnType[0])) {
            body.add(new ReturnNode(null, new NodeContext(line, column, file)));
        }

        // 检查参数名是否重复
        Set<String> seen = new HashSet<>();
        for (ParameterNode node : parameters) {
            if (!seen.add(node.name())) {
                throw new ParseException(
                        String.format("参数 `%s` 重定义", node.name()),
                        node.context().line(),
                        node.context().column());
            }
        }

        // 解析函数结尾
        parseFunctionFooter(ts);

        return new FunctionNode(functionName, parameters, returnType[0], body,
                new NodeContext(line, column, file));
    }

    /* ====================================================================== */
    /* -------------------------- Section Definitions ----------------------- */
    /* ====================================================================== */

    /**
     * 定义函数的各个语义区块及其解析规则，包括参数列表、返回类型及函数体。
     *
     * <p>
     * 此方法将定义如下的三个区块：
     * <ul>
     *   <li>"params" 区块，解析参数列表。</li>
     *   <li>"returns" 区块，解析返回类型。</li>
     *   <li>"body" 区块，解析函数体。</li>
     * </ul>
     * </p>
     *
     * @param params     存储函数参数列表的集合
     * @param returnType 存储函数返回类型的字符串数组
     * @param body       存储函数体的语句列表
     * @return 各个区块的解析定义
     */
    private Map<String, SectionDefinition> getSectionDefinitions(
            List<ParameterNode> params,
            String[] returnType,
            List<StatementNode> body) {
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

    /* ====================================================================== */
    /* ---------------------------- 函数头/尾 ------------------------------- */
    /* ====================================================================== */

    /**
     * 解析函数头部分，期望的格式为 `function:`。
     *
     * @param ts 当前使用的 {@link TokenStream}。
     */
    private void parseFunctionHeader(TokenStream ts) {
        ts.expect("function");
        ts.expect(":");
        skipComments(ts);
        skipNewlines(ts);
    }

    /**
     * 解析函数名称（标识符）并跳过换行。
     *
     * @param ts 当前使用的 {@link TokenStream}。
     * @return 函数名称字符串。
     */
    private String parseFunctionName(TokenStream ts) {
        String name = ts.expectType(TokenType.IDENTIFIER).getLexeme();
        ts.expectType(TokenType.NEWLINE);
        return name;
    }

    /**
     * 解析函数结束标记 `end function`。
     *
     * @param ts 当前使用的 {@link TokenStream}。
     */
    private void parseFunctionFooter(TokenStream ts) {
        ts.expect("end");
        ts.expect("function");
    }

    /**
     * 解析函数参数列表，支持附加注释，格式为：
     * <pre>
     * params:
     *   declare x: int   // 说明文字
     *   declare y: float
     * </pre>
     *
     * @param ctx 当前解析上下文，包含 {@link TokenStream} 和符号表等信息。
     * @return 所有参数节点的列表。
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

            // 既接受 TYPE 也接受 IDENTIFIER
            String ptype;
            if (ts.peek().getType() == TokenType.TYPE || ts.peek().getType() == TokenType.IDENTIFIER) {
                ptype = ts.next().getLexeme();
            } else {
                var t = ts.peek();
                throw new UnexpectedToken(
                        "期望 TYPE 或 IDENTIFIER，但实际得到 " +
                                t.getType() + " ('" + t.getLexeme() + "')",
                        t.getLine(), t.getCol());
            }

            skipComments(ts);
            ts.expectType(TokenType.NEWLINE);
            list.add(new ParameterNode(pname, ptype, new NodeContext(line, column, file)));
        }
        return list;
    }

    /**
     * 解析返回类型声明，格式为 `returns: TYPE`，支持注释。
     *
     * @param ts 当前使用的 {@link TokenStream}。
     * @return 返回类型字符串。
     */
    private String parseReturnType(TokenStream ts) {
        ts.expect("returns");
        ts.expect(":");
        skipComments(ts);

        Token typeToken;
        if (ts.peek().getType() == TokenType.TYPE || ts.peek().getType() == TokenType.IDENTIFIER) {
            typeToken = ts.next();
        } else {
            var t = ts.peek();
            throw new UnexpectedToken(
                    "期望 TYPE 或 IDENTIFIER，但实际得到 " +
                            t.getType() + " ('" + t.getLexeme() + "')",
                    t.getLine(), t.getCol());
        }

        String rtype = typeToken.getLexeme();
        skipComments(ts);
        ts.expectType(TokenType.NEWLINE);
        skipNewlines(ts);
        return rtype;
    }

    /**
     * 解析函数体区块，直到遇到 `end body`。
     *
     * @param ctx 当前解析上下文。
     * @param ts  当前使用的 {@link TokenStream}。
     * @return 所有函数体语句节点的列表。
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
     * 跳过连续的注释 token（类型 {@code COMMENT}）。
     *
     * @param ts 当前使用的 {@link TokenStream}。
     */
    private void skipComments(TokenStream ts) {
        while (ts.peek().getType() == TokenType.COMMENT) ts.next();
    }

    /**
     * 跳过连续的空行 token（类型 {@code NEWLINE}）。
     *
     * @param ts 当前使用的 {@link TokenStream}。
     */
    private void skipNewlines(TokenStream ts) {
        while (ts.peek().getType() == TokenType.NEWLINE) ts.next();
    }
}
