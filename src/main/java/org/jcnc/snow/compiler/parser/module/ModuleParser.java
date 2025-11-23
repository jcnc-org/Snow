package org.jcnc.snow.compiler.parser.module;

import org.jcnc.snow.compiler.lexer.token.TokenType;
import org.jcnc.snow.compiler.parser.ast.*;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.parser.base.TopLevelParser;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.context.TokenStream;
import org.jcnc.snow.compiler.parser.context.UnexpectedToken;
import org.jcnc.snow.compiler.parser.context.UnsupportedFeature;
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
 */
public class ModuleParser implements TopLevelParser {

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
            if (ts.peek().getType() == TokenType.NEWLINE) {
                ts.next();
                continue;
            }
            if ("end".equals(ts.peek().getLexeme())) {
                break;
            }
            String lex = ts.peek().getLexeme();
            switch (lex) {
                case "import" -> imports.addAll(importParser.parse(ctx));
                case "struct" -> structs.add(structParser.parse(ctx));
                case "function" -> functions.add(funcParser.parse(ctx));
                case "globals" -> {
                    ts.expect("globals");
                    ts.expect(":");
                    ts.expectType(TokenType.NEWLINE);
                    while (true) {
                        if (ts.peek().getType() == TokenType.NEWLINE) {
                            ts.next();
                            continue;
                        }
                        String innerLex = ts.peek().getLexeme();
                        if ("declare".equals(innerLex)) {
                            globals.add(globalsParser.parse(ctx));
                        } else if ("function".equals(innerLex)
                                || "import".equals(innerLex)
                                || "struct".equals(innerLex)
                                || "end".equals(innerLex)) {
                            break;
                        } else {
                            throw new UnexpectedToken(
                                    "globals 区块中不支持的内容: " + innerLex,
                                    ts.peek().getLine(),
                                    ts.peek().getCol()
                            );
                        }
                    }
                }
                default -> throw new UnexpectedToken(
                        "Unexpected token in module: " + lex,
                        ts.peek().getLine(),
                        ts.peek().getCol()
                );
            }
        }

        // 5) 匹配模块结尾 "end module"
        ts.expect("end");
        ts.expect("module");

        // 6) 校验 module 名与文件名一致性 (fd ↔ fd.snow)
        String baseName;
        try {
            java.nio.file.Path p = java.nio.file.Paths.get(file);
            baseName = (p.getFileName() == null) ? file : p.getFileName().toString();
        } catch (Exception e) {
            baseName = file;
        }

        if ("fd".equals(name) && !"fd.snow".equals(baseName)) {
            throw new UnsupportedFeature(
                    "模块名为 \"fd\" 时，文件名必须为 \"fd.snow\"，实际文件: " + baseName,
                    line, column
            );
        }
        if ("fd.snow".equals(baseName) && !"fd".equals(name)) {
            throw new UnsupportedFeature(
                    "当文件名为 \"fd.snow\" 时，模块名必须为 \"fd\"，实际模块名: " + name,
                    line, column
            );
        }

        // 7) 返回构建的 ModuleNode
        return new ModuleNode(name, imports, globals, structs, functions,
                new NodeContext(line, column, file));
    }
}
