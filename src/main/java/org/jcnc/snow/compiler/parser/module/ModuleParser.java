package org.jcnc.snow.compiler.parser.module;

import org.jcnc.snow.compiler.lexer.token.TokenType;
import org.jcnc.snow.compiler.parser.ast.*;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.parser.base.TopLevelParser;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.context.TokenStream;
import org.jcnc.snow.compiler.parser.context.UnexpectedToken;
import org.jcnc.snow.compiler.parser.function.FunctionParser;
import org.jcnc.snow.compiler.parser.statement.DeclarationStatementParser;
import org.jcnc.snow.compiler.parser.struct.StructParser;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code ModuleParser}
 * <p>
 * 顶层结构解析器：负责解析整个源码模块（module ... end module）。
 * <ul>
 *   <li>支持模块声明、导入(import)、全局变量(globals)、结构体(struct)、函数(function)等顶层语法。</li>
 *   <li>允许模块体中出现任意数量的空行（自动跳过），顺序自由。</li>
 *   <li>遇到非法顶层语句或区块会抛出 {@link UnexpectedToken}，提示具体位置和原因。</li>
 * </ul>
 * </p>
 */
public class ModuleParser implements TopLevelParser {

    /**
     * 解析一个完整的模块定义块，返回 AST {@link ModuleNode}。
     * <p>
     * 支持空行，允许导入、全局、结构体、函数等多种区块混排。
     * </p>
     *
     * @param ctx 解析上下文（包含 TokenStream、文件名等信息）
     * @return 解析生成的 ModuleNode
     * @throws UnexpectedToken 当模块体中遇到不支持的顶层语句时抛出
     */
    @Override
    public ModuleNode parse(ParserContext ctx) {
        TokenStream ts = ctx.getTokens();

        int line = ts.peek().getLine();
        int column = ts.peek().getCol();
        String file = ctx.getSourceName();

        // 1) 解析模块声明头部
        ts.expect("module");
        ts.expect(":");
        String name = ts.expectType(TokenType.IDENTIFIER).getLexeme();
        ts.expectType(TokenType.NEWLINE);

        // 2) 初始化各类节点容器
        List<StructNode> structs = new ArrayList<>();
        List<ImportNode> imports = new ArrayList<>();
        List<DeclarationNode> globals = new ArrayList<>();
        List<FunctionNode> functions = new ArrayList<>();

        // 3) 各子区块的专用解析器
        StructParser structParser = new StructParser();
        ImportParser importParser = new ImportParser();
        FunctionParser funcParser = new FunctionParser();
        DeclarationStatementParser globalsParser = new DeclarationStatementParser();

        // 4) 进入主循环，直到 end module
        while (true) {
            // 跳过空行
            if (ts.peek().getType() == TokenType.NEWLINE) {
                ts.next();
                continue;
            }
            // 到达模块结尾
            if ("end".equals(ts.peek().getLexeme())) {
                break;
            }
            String lex = ts.peek().getLexeme();
            switch (lex) {
                // 解析 import 语句（可多次出现，支持 import 多个模块）
                case "import" -> imports.addAll(importParser.parse(ctx));

                // 解析 struct 结构体定义块
                case "struct" -> structs.add(structParser.parse(ctx));

                // 解析 function 顶层函数定义
                case "function" -> functions.add(funcParser.parse(ctx));

                // 解析全局变量声明区块
                case "globals" -> {
                    ts.expect("globals");
                    ts.expect(":");
                    ts.expectType(TokenType.NEWLINE);
                    while (true) {
                        // 跳过空行
                        if (ts.peek().getType() == TokenType.NEWLINE) {
                            ts.next();
                            continue;
                        }
                        String innerLex = ts.peek().getLexeme();
                        if ("declare".equals(innerLex)) {
                            globals.add(globalsParser.parse(ctx));
                        }
                        // 下一个 function/import/end 开头则结束 globals 区块
                        else if ("function".equals(innerLex) || "import".equals(innerLex) || "end".equals(innerLex)) {
                            break;
                        }
                        // 其余标记为非法内容，抛出异常
                        else {
                            throw new UnexpectedToken(
                                    "globals 区块中不支持的内容: " + innerLex,
                                    ts.peek().getLine(),
                                    ts.peek().getCol()
                            );
                        }
                    }
                }
                // 未知或非法顶层内容
                case null, default -> throw new UnexpectedToken(
                        "Unexpected token in module: " + lex,
                        ts.peek().getLine(),
                        ts.peek().getCol()
                );
            }
        }

        // 5) 匹配模块结尾 "end module"
        ts.expect("end");
        ts.expect("module");

        // 6) 构造并返回 ModuleNode
        return new ModuleNode(name, imports, globals, structs, functions, new NodeContext(line, column, file));
    }
}
