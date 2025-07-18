package org.jcnc.snow.compiler.parser.module;

import org.jcnc.snow.compiler.lexer.token.TokenType;
import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.parser.ast.ImportNode;
import org.jcnc.snow.compiler.parser.ast.ModuleNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.parser.base.TopLevelParser;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.context.TokenStream;
import org.jcnc.snow.compiler.parser.context.UnexpectedToken;
import org.jcnc.snow.compiler.parser.function.FunctionParser;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code ModuleParser} 负责解析源码中的模块结构，是顶层结构解析器实现之一。
 * <p>
 * 模块定义可包含多个导入（import）语句和函数定义（function），
 * 导入语句可在模块中任意位置出现，且允许模块体中穿插任意数量的空行（空行会被自动忽略，不影响语法结构）。
 * </p>
 *
 * <p>
 * 典型模块语法结构:
 * <pre>
 * module: mymod
 *   import ...
 *   function ...
 *   ...
 * end module
 * </pre>
 * </p>
 */
public class ModuleParser implements TopLevelParser {

    /**
     * 解析一个模块定义块，返回完整的 {@link ModuleNode} 语法树节点。
     * <p>
     * 解析过程包括:
     * <ol>
     *     <li>匹配模块声明起始 {@code module: IDENTIFIER}。</li>
     *     <li>收集模块体内所有 import 和 function 语句，允许穿插空行。</li>
     *     <li>匹配模块结束 {@code end module}。</li>
     * </ol>
     * 若遇到未识别的语句，将抛出 {@link UnexpectedToken} 异常，定位错误位置和原因。
     * </p>
     *
     * @param ctx 当前解析上下文（包含词法流等状态）
     * @return 解析得到的 {@link ModuleNode} 实例
     * @throws UnexpectedToken 当模块体中出现未识别的顶层语句时抛出
     */
    @Override
    public ModuleNode parse(ParserContext ctx) {
        TokenStream ts = ctx.getTokens();

        int line = ts.peek().getLine();
        int column = ts.peek().getCol();
        String file = ctx.getSourceName();

        ts.expect("module");
        ts.expect(":");
        String name = ts.expectType(TokenType.IDENTIFIER).getLexeme();
        ts.expectType(TokenType.NEWLINE);

        List<ImportNode> imports = new ArrayList<>();
        List<FunctionNode> functions = new ArrayList<>();

        ImportParser importParser = new ImportParser();
        FunctionParser funcParser = new FunctionParser();

        while (true) {
            if (ts.peek().getType() == TokenType.NEWLINE) {
                ts.next();
                continue;
            }
            if ("end".equals(ts.peek().getLexeme())) {
                break;
            }
            String lex = ts.peek().getLexeme();
            if ("import".equals(lex)) {
                imports.addAll(importParser.parse(ctx));
            } else if ("function".equals(lex)) {
                functions.add(funcParser.parse(ctx));
            } else {
                throw new UnexpectedToken(
                        "Unexpected token in module: " + lex,
                        ts.peek().getLine(),
                        ts.peek().getCol()
                );
            }
        }

        ts.expect("end");
        ts.expect("module");

        return new ModuleNode(name, imports, functions, new NodeContext(line, column, file));
    }
}
