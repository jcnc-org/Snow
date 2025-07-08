package org.jcnc.snow.compiler.parser.function;

import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.lexer.token.TokenType;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.parser.base.TopLevelParser;
import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.parser.ast.ParameterNode;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.context.TokenStream;
import org.jcnc.snow.compiler.parser.factory.StatementParserFactory;
import org.jcnc.snow.compiler.parser.utils.FlexibleSectionParser;
import org.jcnc.snow.compiler.parser.utils.FlexibleSectionParser.SectionDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code FunctionParser} 是顶层函数定义的语法解析器，
 * 实现 {@link TopLevelParser} 接口，用于将源代码中的函数块解析为抽象语法树（AST）中的 {@link FunctionNode}。
 *
 * <p>
 * 本类使用 {@link FlexibleSectionParser} 机制，按照语义区块结构对函数进行模块化解析，支持以下部分：
 * </p>
 *
 * <ul>
 *     <li>函数头（关键字 {@code function:} 与函数名）</li>
 *     <li>参数列表（parameter 区块）</li>
 *     <li>返回类型（return_type 区块）</li>
 *     <li>函数体（body 区块）</li>
 *     <li>函数结束（关键字 {@code end function}）</li>
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
     * 该方法负责完整解析函数定义，包括其所有组成部分，并构建对应的 {@link FunctionNode}。
     * </p>
     *
     * @param ctx 当前解析上下文，包含 {@link TokenStream} 和符号表等作用域信息。
     * @return 构建完成的 {@link FunctionNode} 抽象语法树节点。
     */
    @Override
    public FunctionNode parse(ParserContext ctx) {
        TokenStream ts = ctx.getTokens();

        // 获取当前 token 的行号、列号和文件名
        int line = ctx.getTokens().peek().getLine();
        int column = ctx.getTokens().peek().getCol();
        String file = ctx.getSourceName();

        parseFunctionHeader(ts);
        String functionName = parseFunctionName(ts);

        List<ParameterNode> parameters = new ArrayList<>();
        String[] returnType = new String[1];
        List<StatementNode> body = new ArrayList<>();

        Map<String, SectionDefinition> sections = getSectionDefinitions(parameters, returnType, body);
        FlexibleSectionParser.parse(ctx, ts, sections);

        parseFunctionFooter(ts);

        return new FunctionNode(functionName, parameters, returnType[0], body, new NodeContext(line, column, file));
    }

    /**
     * 构造函数定义中各区块的解析规则（parameter、return_type、body）。
     *
     * <p>
     * 每个 {@link SectionDefinition} 包含两个部分：区块起始判断器（基于关键字）与具体的解析逻辑。
     * </p>
     *
     * @param params 参数节点收集容器，解析结果将存入此列表。
     * @param returnType 返回类型容器，以单元素数组方式模拟引用传递。
     * @param body 函数体语句节点列表容器。
     * @return 区块关键字到解析定义的映射表。
     */
    private Map<String, SectionDefinition> getSectionDefinitions(
            List<ParameterNode> params,
            String[] returnType,
            List<StatementNode> body) {
        Map<String, SectionDefinition> map = new HashMap<>();

        map.put("parameter", new SectionDefinition(
                (TokenStream stream) -> stream.peek().getLexeme().equals("parameter"),
                (ParserContext context, TokenStream stream) -> params.addAll(parseParameters(context))
        ));

        map.put("return_type", new SectionDefinition(
                (TokenStream stream) -> stream.peek().getLexeme().equals("return_type"),
                (ParserContext context, TokenStream stream) -> returnType[0] = parseReturnType(stream)
        ));

        map.put("body", new SectionDefinition(
                (TokenStream stream) -> stream.peek().getLexeme().equals("body"),
                (ParserContext context, TokenStream stream) -> body.addAll(parseFunctionBody(context, stream))
        ));

        return map;
    }

    /**
     * 解析函数头部标识符 {@code function:}，并跳过其后多余注释与空行。
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
     * @return 函数名字符串。
     */
    private String parseFunctionName(TokenStream ts) {
        String name = ts.expectType(TokenType.IDENTIFIER).getLexeme();
        ts.expectType(TokenType.NEWLINE);
        return name;
    }

    /**
     * 解析函数结束标记 {@code end function}。
     *
     * @param ts 当前使用的 {@link TokenStream}。
     */
    private void parseFunctionFooter(TokenStream ts) {
        ts.expect("end");
        ts.expect("function");
    }

    /**
     * 解析函数参数列表。
     *
     * <p>
     * 支持声明后附加注释，格式示例：
     * <pre>
     * parameter:
     *   declare x: int   // 说明文字
     *   declare y: float
     * </pre>
     * </p>
     *
     * @param ctx 当前解析上下文，包含 {@link TokenStream} 和符号表等作用域信息。
     * @return 所有参数节点的列表。
     */
    private List<ParameterNode> parseParameters(ParserContext ctx) {
        TokenStream ts = ctx.getTokens();

        ts.expect("parameter");
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
            if (lex.equals("return_type") || lex.equals("body") || lex.equals("end")) {
                break;
            }

            // 获取当前 token 的行号、列号和文件名
            int line = ctx.getTokens().peek().getLine();
            int column = ctx.getTokens().peek().getCol();
            String file = ctx.getSourceName();

            ts.expect("declare");
            String pname = ts.expectType(TokenType.IDENTIFIER).getLexeme();
            ts.expect(":");
            String ptype = ts.expectType(TokenType.TYPE).getLexeme();
            skipComments(ts);
            ts.expectType(TokenType.NEWLINE);
            list.add(new ParameterNode(pname, ptype, new NodeContext(line, column, file)));
        }
        return list;
    }

    /**
     * 解析返回类型声明。
     *
     * <p>
     * 格式为 {@code return_type: TYPE}，支持前置或行尾注释。
     * </p>
     *
     * @param ts 当前使用的 {@link TokenStream}。
     * @return 返回类型名称字符串。
     */
    private String parseReturnType(TokenStream ts) {
        ts.expect("return_type");
        ts.expect(":");
        skipComments(ts);
        Token typeToken = ts.expectType(TokenType.TYPE);
        String rtype = typeToken.getLexeme();
        skipComments(ts);
        ts.expectType(TokenType.NEWLINE);
        skipNewlines(ts);
        return rtype;
    }

    /**
     * 解析函数体区块，直到遇到 {@code end body}。
     *
     * <p>
     * 每一行由对应的语句解析器处理，可嵌套控制结构、返回语句、表达式等。
     * </p>
     *
     * @param ctx 当前解析上下文。
     * @param ts 当前使用的 {@link TokenStream}。
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
            if ("end".equals(ts.peek().getLexeme())) {
                break;
            }
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
        while (ts.peek().getType() == TokenType.COMMENT) {
            ts.next();
        }
    }

    /**
     * 跳过连续的空行 token（类型 {@code NEWLINE}）。
     *
     * @param ts 当前使用的 {@link TokenStream}。
     */
    private void skipNewlines(TokenStream ts) {
        while (ts.peek().getType() == TokenType.NEWLINE) {
            ts.next();
        }
    }
}