package org.jcnc.snow.compiler.parser.module;

import org.jcnc.snow.compiler.lexer.token.TokenType;
import org.jcnc.snow.compiler.parser.base.TopLevelParser;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.context.TokenStream;
import org.jcnc.snow.compiler.parser.ast.ImportNode;
import org.jcnc.snow.compiler.parser.ast.ModuleNode;
import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.parser.function.FunctionParser;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code ModuleParser} 类负责解析源码中的模块定义结构，属于顶层结构解析器的一种。
 * <p>
 * 模块中可包含多个导入语句和函数定义，导入语句可在模块中任意位置出现，
 * 同时支持空行，空行将被自动忽略，不影响语法结构的正确性。
 */
public class ModuleParser implements TopLevelParser {

    /**
     * 解析一个模块定义块，返回构建好的 {@link ModuleNode} 对象。
     * <p>
     * 本方法的语法流程包括：
     * <ol>
     *     <li>匹配模块声明开头 {@code module: IDENTIFIER}。</li>
     *     <li>收集模块体中的 import 语句与 function 定义，允许穿插空行。</li>
     *     <li>模块结尾必须为 {@code end module}，且后接换行符。</li>
     * </ol>
     * 所有语法错误将在解析过程中抛出异常，以便准确反馈问题位置和原因。
     *
     * @param ctx 当前解析器上下文，包含词法流、状态信息等。
     * @return 返回一个 {@link ModuleNode} 实例，表示完整模块的语法结构。
     * @throws IllegalStateException 当模块体中出现未识别的语句时抛出。
     */
    @Override
    public ModuleNode parse(ParserContext ctx) {
        // 获取当前上下文中提供的词法流
        TokenStream ts = ctx.getTokens();

        // 期望模块声明以关键字 "module:" 开始
        ts.expect("module");
        ts.expect(":");

        // 读取模块名称（要求为标识符类型的词法单元）
        String name = ts.expectType(TokenType.IDENTIFIER).getLexeme();

        // 模块声明必须以换行符结束
        ts.expectType(TokenType.NEWLINE);

        // 初始化模块的导入节点列表与函数节点列表
        List<ImportNode> imports = new ArrayList<>();
        List<FunctionNode> functions = new ArrayList<>();

        // 创建 import 与 function 的子解析器
        ImportParser importParser = new ImportParser();
        FunctionParser funcParser = new FunctionParser();

        // 进入模块主体内容解析循环
        while (true) {
            // 跳过所有空行（即连续的 NEWLINE）
            if (ts.peek().getType() == TokenType.NEWLINE) {
                ts.next();
                continue;
            }

            // 若遇到 "end"，则表明模块定义结束
            if ("end".equals(ts.peek().getLexeme())) {
                break;
            }

            // 根据当前行首关键字决定解析器的选择
            String lex = ts.peek().getLexeme();
            if ("import".equals(lex)) {
                // 调用导入语句解析器，解析多个模块导入节点
                imports.addAll(importParser.parse(ctx));
            } else if ("function".equals(lex)) {
                // 调用函数定义解析器，解析单个函数结构
                functions.add(funcParser.parse(ctx));
            } else {
                // 遇到无法识别的语句开头，抛出异常并提供详细提示
                throw new IllegalStateException("Unexpected token in module: " + lex);
            }
        }

        // 确保模块体以 "end module" 结束
        ts.expect("end");
        ts.expect("module");

        // 构建并返回完整的模块语法树节点
        return new ModuleNode(name, imports, functions);
    }
}