package org.jcnc.snow.compiler.parser.utils;

import org.jcnc.snow.compiler.lexer.token.TokenType;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.context.TokenStream;
import org.jcnc.snow.compiler.parser.context.UnexpectedToken;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * {@code FlexibleSectionParser} 是一个通用的语法块解析工具。
 * <p>
 * 该工具支持解析由关键字标识的多段结构化区块内容，常用于解析函数、类、模块、循环等语法单元中的命名子结构。
 * 相比传统硬编码方式，提供更灵活、可组合的解析能力，允许解析器模块动态注册处理逻辑，而非将所有逻辑写死在主流程中。
 *
 * <p>典型应用包括：
 * <ul>
 *   <li>函数体解析中的 {@code params}、{@code returns}、{@code body} 等部分</li>
 *   <li>模块定义中的 {@code imports}、{@code functions} 等部分</li>
 *   <li>用户自定义 DSL 的可扩展语法结构</li>
 * </ul>
 *
 * <p>该工具具备以下能力：
 * <ul>
 *   <li>自动跳过注释与空行</li>
 *   <li>根据区块名称调用外部提供的解析器</li>
 *   <li>支持终止标志（如 {@code end}）来退出解析流程</li>
 * </ul>
 */
public class FlexibleSectionParser {

    /**
     * 启动结构化区块的统一解析流程。
     * <p>
     * 每次调用会：
     * <ol>
     *   <li>从 token 流中跳过空行与注释</li>
     *   <li>依照当前 token 判断是否匹配某个区块</li>
     *   <li>调用对应 {@link SectionDefinition} 执行区块解析逻辑</li>
     *   <li>若遇到 {@code end} 关键字，则终止解析过程</li>
     *   <li>若当前 token 不匹配任何已注册区块，抛出异常</li>
     * </ol>
     *
     * @param ctx                当前解析上下文，提供语法环境与作用域信息
     * @param tokens             当前 token 流
     * @param sectionDefinitions 各个区块的定义映射（key 为关键字，value 为判断 + 解析逻辑组合）
     * @throws UnexpectedToken 若出现无法识别的关键字或未满足的匹配条件
     */
    public static void parse(ParserContext ctx,
                             TokenStream tokens,
                             Map<String, SectionDefinition> sectionDefinitions) {

        // 跳过开头的注释或空行
        skipCommentsAndNewlines(tokens);

        while (true) {
            // 跳过当前区块之间的空白与注释
            skipCommentsAndNewlines(tokens);

            String keyword = tokens.peek().getLexeme();

            // 结束关键字表示解析流程终止
            if ("end".equals(keyword)) {
                break;
            }

            // 查找匹配的区块定义
            SectionDefinition definition = sectionDefinitions.get(keyword);
            if (definition != null && definition.condition().test(tokens)) {
                definition.parser().accept(ctx, tokens); // 执行解析逻辑
            } else {
                throw new UnexpectedToken("未识别的关键字或条件不满足: " + keyword);
            }
        }
    }

    /**
     * 跳过连续出现的注释行或空行（NEWLINE）。
     * <p>
     * 该方法用于在区块之间清理无效 token，避免影响结构判断。
     *
     * @param tokens 当前 token 流
     */
    private static void skipCommentsAndNewlines(TokenStream tokens) {
        while (true) {
            TokenType type = tokens.peek().getType();
            if (type == TokenType.COMMENT || type == TokenType.NEWLINE) {
                tokens.next(); // 跳过注释或换行
                continue;
            }
            break;
        }
    }

    /**
     * 表示一个结构区块的定义，包含匹配条件与解析器。
     * <p>
     * 每个区块由两部分组成：
     * <ul>
     *   <li>{@code condition}：用于判断当前 token 是否应进入该区块</li>
     *   <li>{@code parser}：该区块对应的实际解析逻辑</li>
     * </ul>
     * 可实现懒加载、多语言支持或 DSL 的结构化扩展。
     *
     * @param condition 判断是否触发该区块的谓词函数
     * @param parser    区块解析逻辑（消费语法上下文与 token 流）
     */
    public record SectionDefinition(Predicate<TokenStream> condition,
                                    BiConsumer<ParserContext, TokenStream> parser) {
        @Override
        public Predicate<TokenStream> condition() {
            return condition;
        }

        @Override
        public BiConsumer<ParserContext, TokenStream> parser() {
            return parser;
        }
    }
}
