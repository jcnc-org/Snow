package org.jcnc.snow.compiler.parser.utils;

import org.jcnc.snow.compiler.lexer.token.TokenType;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.context.TokenStream;
import org.jcnc.snow.compiler.parser.context.UnexpectedToken;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * {@code FlexibleSectionParser} 是一个通用的区块（Section）解析工具。
 * <p>
 * 支持通过注册表驱动的方式解析具有区块关键字标识的多段结构内容，
 * 常用于函数、类、模块、循环等语法单元中的命名子结构。
 * 通过外部注册解析逻辑，支持高度可扩展与复用。
 * </p>
 *
 * <p>
 * 典型用途包括：
 * <ul>
 *   <li>函数体解析中的 {@code params}、{@code returns}、{@code body} 等部分</li>
 *   <li>模块定义中的 {@code imports}、{@code functions} 等部分</li>
 *   <li>可扩展 DSL 的结构化语法区块</li>
 * </ul>
 * </p>
 *
 * <p>主要特性：</p>
 * <ul>
 *   <li>自动跳过注释与空行</li>
 *   <li>区块入口通过关键字匹配和可选条件判断</li>
 *   <li>解析逻辑由外部以函数式接口方式注册</li>
 *   <li>支持遇到终止关键字（如 {@code end}）时自动停止</li>
 * </ul>
 */
public class FlexibleSectionParser {

    /**
     * 解析并分派处理多区块结构。
     *
     * @param ctx                解析上下文
     * @param tokens             词法流
     * @param sectionDefinitions 区块处理注册表，key 为区块关键字，value 为对应的处理定义
     * @throws UnexpectedToken   遇到未注册或条件不符的关键字时抛出
     */
    public static void parse(ParserContext ctx,
                             TokenStream tokens,
                             Map<String, SectionDefinition> sectionDefinitions) {

        skipCommentsAndNewlines(tokens);

        while (true) {
            skipCommentsAndNewlines(tokens);

            String keyword = tokens.peek().getLexeme();

            if ("end".equals(keyword)) {
                break;
            }

            SectionDefinition definition = sectionDefinitions.get(keyword);
            if (definition != null && definition.condition().test(tokens)) {
                definition.parser().accept(ctx, tokens);
            } else {
                throw new UnexpectedToken(
                        "未识别的关键字或条件不满足: " + keyword,
                        tokens.peek().getLine(),
                        tokens.peek().getCol()
                );
            }
        }
    }

    /**
     * 跳过所有连续的注释（COMMENT）和空行（NEWLINE）token。
     *
     * @param tokens 当前词法流
     */
    private static void skipCommentsAndNewlines(TokenStream tokens) {
        while (true) {
            TokenType type = tokens.peek().getType();
            if (type == TokenType.COMMENT || type == TokenType.NEWLINE) {
                tokens.next();
                continue;
            }
            break;
        }
    }

    /**
     * 区块定义，包含进入区块的判断条件与具体解析逻辑。
     *
     * @param condition 匹配区块的前置条件
     * @param parser    区块内容的具体解析操作
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
