package org.jcnc.snow.compiler.parser.base;

import org.jcnc.snow.compiler.parser.ast.base.Node;
import org.jcnc.snow.compiler.parser.context.ParserContext;

/**
 * {@code TopLevelParser} 是顶层语法结构的解析器接口。
 * <p>
 * 用于解析模块级别的构造，例如 {@code module}、{@code import}、{@code function} 等。
 * 所有顶层语法解析器应实现该接口，并从 {@link ParserContext} 提供的 TokenStream 中提取并构建 AST 节点。
 * </p>
 * <p>
 * 本接口由 {@link org.jcnc.snow.compiler.parser.factory.TopLevelParserFactory} 负责根据关键字进行动态分派，
 * 以便支持扩展性与模块化解析策略。
 * </p>
 */
public interface TopLevelParser {

    /**
     * 从解析上下文中解析一个顶层语法结构节点。
     * <p>
     * 每个实现类应根据自身语法规则消费 TokenStream 中的相应 token，
     * 构造对应的 AST 子树结构。
     * </p>
     *
     * @param ctx 当前解析上下文，包含 Token 流与中间状态
     * @return 构建完成的 AST 节点，不应为 {@code null}
     * @throws IllegalStateException 若解析过程遇到非法结构或上下文状态异常
     */
    Node parse(ParserContext ctx);
}