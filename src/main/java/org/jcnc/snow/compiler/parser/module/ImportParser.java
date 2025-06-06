package org.jcnc.snow.compiler.parser.module;

import org.jcnc.snow.compiler.lexer.token.TokenType;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.ast.ImportNode;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code ImportParser} 类用于解析源码中的 import 导入语句。
 * <p>
 * 支持以下格式的语法：
 * <pre>
 * import: module1, module2, module3
 * </pre>
 * 每个模块名称必须为合法的标识符，多个模块之间使用英文逗号（,）分隔，语句末尾必须以换行符结尾。
 * 本类的职责是识别并提取所有导入模块的名称，并将其封装为 {@link ImportNode} 节点，供后续语法树构建或语义分析阶段使用。
 */
public class ImportParser {

    /**
     * 解析 import 语句，并返回表示被导入模块的语法树节点列表。
     * <p>
     * 该方法会依次执行以下操作：
     * <ol>
     *     <li>确认当前语句以关键字 {@code import} 开头。</li>
     *     <li>确认后跟一个冒号 {@code :}。</li>
     *     <li>解析至少一个模块名称（标识符），多个模块使用逗号分隔。</li>
     *     <li>确认语句以换行符 {@code NEWLINE} 结束。</li>
     * </ol>
     * 若语法不符合上述规则，将在解析过程中抛出异常。
     *
     * @param ctx 表示当前解析器所处的上下文环境，包含词法流及语法状态信息。
     * @return 返回一个包含所有被导入模块的 {@link ImportNode} 实例列表。
     */
    public List<ImportNode> parse(ParserContext ctx) {
        // 期望第一个 token 是 "import" 关键字
        ctx.getTokens().expect("import");

        // 紧接其后必须是冒号 ":"
        ctx.getTokens().expect(":");

        // 用于存储解析得到的 ImportNode 对象
        List<ImportNode> imports = new ArrayList<>();

        // 解析一个或多个模块名（标识符），允许使用逗号分隔多个模块
        do {
            // 获取当前标识符类型的词法单元，并提取其原始词素
            String mod = ctx.getTokens()
                    .expectType(TokenType.IDENTIFIER)
                    .getLexeme();

            // 创建 ImportNode 节点并加入列表
            imports.add(new ImportNode(mod));
        } while (ctx.getTokens().match(",")); // 如果匹配到逗号，继续解析下一个模块名

        // 最后必须匹配换行符，标志 import 语句的结束
        ctx.getTokens().expectType(TokenType.NEWLINE);

        // 返回完整的 ImportNode 列表
        return imports;
    }
}
