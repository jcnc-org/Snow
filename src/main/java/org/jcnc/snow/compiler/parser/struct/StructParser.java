package org.jcnc.snow.compiler.parser.struct;

import org.jcnc.snow.compiler.lexer.token.TokenType;
import org.jcnc.snow.compiler.parser.ast.DeclarationNode;
import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.parser.ast.ParameterNode;
import org.jcnc.snow.compiler.parser.ast.StructNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;
import org.jcnc.snow.compiler.parser.base.TopLevelParser;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.context.TokenStream;
import org.jcnc.snow.compiler.parser.context.UnexpectedToken;
import org.jcnc.snow.compiler.parser.factory.StatementParserFactory;
import org.jcnc.snow.compiler.parser.function.FunctionParser;
import org.jcnc.snow.compiler.parser.statement.DeclarationStatementParser;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code StructParser}
 * <p>
 * 解析 <code>struct ... end struct</code> 结构体声明块的顶层语法解析器。
 * <ul>
 *   <li>支持解析结构体字段、构造函数（init）、结构体方法列表。</li>
 *   <li>按顺序支持 <b>fields/init/function</b> 三种结构体内部块。</li>
 *   <li>语法出错时抛出 {@link UnexpectedToken} 异常，便于错误定位。</li>
 * </ul>
 * </p>
 */
public class StructParser implements TopLevelParser {

    /**
     * 解析结构体声明块，并返回 AST 节点 {@link StructNode}。
     * <p>
     * @param ctx 解析上下文
     * @return 结构体节点 StructNode
     */
    @Override
    public StructNode parse(ParserContext ctx) {
        TokenStream ts = ctx.getTokens();

        int line = ts.peek().getLine();
        int col = ts.peek().getCol();
        String file = ctx.getSourceName();

        /* -------- 解析头部 -------- */
        ts.expect("struct");
        ts.expect(":");
        String structName = ts.expectType(TokenType.IDENTIFIER).getLexeme();
        ts.expectType(TokenType.NEWLINE);

        /* -------- 初始化容器 -------- */
        List<DeclarationNode> fields = new ArrayList<>();
        FunctionNode init = null;
        List<FunctionNode> methods = new ArrayList<>();

        DeclarationStatementParser declParser = new DeclarationStatementParser();
        FunctionParser funcParser = new FunctionParser();

        /* -------- 主循环：依次解析 struct 块内部字段、构造、方法 -------- */
        while (true) {
            /* 跳过空行 */
            if (ts.peek().getType() == TokenType.NEWLINE) {
                ts.next();
                continue;
            }

            String lex = ts.peek().getLexeme();
            switch (lex) {
                /* ---------- fields 块 ---------- */
                case "fields" -> {
                    ts.expect("fields");
                    ts.expect(":");
                    ts.expectType(TokenType.NEWLINE);
                    while (true) {
                        if (ts.peek().getType() == TokenType.NEWLINE) {
                            ts.next();
                            continue;
                        }
                        if ("declare".equals(ts.peek().getLexeme())) {
                            fields.add(declParser.parse(ctx));
                        } else { // 非 declare 开头则结束 fields
                            break;
                        }
                    }
                }

                /* ---------- 构造函数 init ---------- */
                case "init" -> {
                    if (init != null) {
                        throw new UnexpectedToken(
                                "重复定义 init 构造函数",
                                ts.peek().getLine(), ts.peek().getCol());
                    }
                    init = parseInit(ctx, structName);
                }

                /* ---------- 普通方法 function ---------- */
                case "function" -> methods.add(funcParser.parse(ctx));

                /* ---------- struct 结束 ---------- */
                case "end" -> {
                    ts.expect("end");
                    ts.expect("struct");
                    // 返回完整结构体 AST 节点
                    return new StructNode(structName, fields, init, methods,
                            new NodeContext(line, col, file));
                }

                /* ---------- 非法内容 ---------- */
                default -> throw new UnexpectedToken(
                        "struct 块内不支持的标记: " + lex,
                        ts.peek().getLine(), ts.peek().getCol());
            }
        }
    }

    /* ====================================================================== */
    /* --------------------------   构造函数 init   -------------------------- */
    /* ====================================================================== */

    /**
     * 解析结构体构造函数 init 块，返回 FunctionNode。
     * <p>
     * 允许包含 params 和 body 两部分。严格要求 "end init" 结束。
     *
     * @param ctx        解析上下文
     * @param structName 结构体名称，用于构造函数唯一命名
     * @return 表示构造函数的 FunctionNode
     */
    private FunctionNode parseInit(ParserContext ctx, String structName) {
        TokenStream ts = ctx.getTokens();

        int line = ts.peek().getLine();
        int col = ts.peek().getCol();
        String file = ctx.getSourceName();

        ts.expect("init");
        ts.expect(":");
        ts.expectType(TokenType.NEWLINE);

        /* -------- 初始化参数和方法体容器 -------- */
        List<ParameterNode> params = new ArrayList<>();
        List<org.jcnc.snow.compiler.parser.ast.base.StatementNode> body = new ArrayList<>();

        // 主循环：支持 params/body 两块，顺序不限
        while (true) {
            if (ts.peek().getType() == TokenType.NEWLINE) {
                ts.next();
                continue;
            }

            String lex = ts.peek().getLexeme();
            switch (lex) {
                case "params" -> params.addAll(parseParams(ctx));
                case "body" -> body.addAll(parseBody(ctx));
                case "end" -> {
                    ts.expect("end");
                    ts.expect("init");
                    // 构造唯一命名的 FunctionNode
                    return new FunctionNode(
                            structName + ".__init__",   // 唯一命名
                            params,
                            "void",
                            body,
                            new NodeContext(line, col, file));
                }
                default -> throw new UnexpectedToken(
                        "init 块内不支持的标记: " + lex,
                        ts.peek().getLine(), ts.peek().getCol());
            }
        }
    }

    /* ---------------- params: 参数块解析 ---------------- */
    /**
     * 解析 params 块，返回参数列表。
     *
     * @param ctx 解析上下文
     * @return 解析得到的参数节点列表
     */
    private List<ParameterNode> parseParams(ParserContext ctx) {
        TokenStream ts = ctx.getTokens();

        ts.expect("params");
        ts.expect(":");
        ts.expectType(TokenType.NEWLINE);

        List<ParameterNode> list = new ArrayList<>();
        while (true) {
            if (ts.peek().getType() == TokenType.NEWLINE) {
                ts.next();
                continue;
            }
            if (ts.peek().getType() != TokenType.IDENTIFIER) break;

            int line = ts.peek().getLine();
            int col = ts.peek().getCol();

            String pName = ts.expectType(TokenType.IDENTIFIER).getLexeme();
            ts.expect(":");
            String pType = ts.expectType(TokenType.TYPE).getLexeme();
            ts.expectType(TokenType.NEWLINE);
            list.add(new ParameterNode(pName, pType, new NodeContext(line, col,
                    ctx.getSourceName())));
        }
        return list;
    }

    /* ---------------- body: 方法体块解析 ---------------- */
    /**
     * 解析 body 块，返回语句节点列表。
     *
     * @param ctx 解析上下文
     * @return 解析得到的方法体语句列表
     */
    private List<org.jcnc.snow.compiler.parser.ast.base.StatementNode> parseBody(ParserContext ctx) {
        TokenStream ts = ctx.getTokens();

        ts.expect("body");
        ts.expect(":");
        ts.expectType(TokenType.NEWLINE);

        List<StatementNode> body = new ArrayList<>();

        // 循环读取每一条语句，直到 end body
        while (true) {
            if (ts.peek().getType() == TokenType.NEWLINE) {
                ts.next();
                continue;
            }
            if ("end".equals(ts.peek().getLexeme())) break;   // body 块结束

            var parser = StatementParserFactory.get(ts.peek().getLexeme());
            body.add(parser.parse(ctx));
        }

        ts.expect("end");
        ts.expect("body");
        return body;
    }
}
