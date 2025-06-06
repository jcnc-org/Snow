package org.jcnc.snow.compiler.parser.context;

import org.jcnc.snow.compiler.lexer.token.Token;
import java.util.List;

/**
 * {@code ParserContext} 表示语法分析阶段的共享上下文容器。
 * <p>
 * 封装了词法单元流（TokenStream），供各级语法解析器读取与回退。
 * 后续还可扩展为包含错误收集、符号表管理、作用域追踪等功能模块，
 * 以支持完整的编译前端功能。
 * </p>
 */
public class ParserContext {

    /** 当前语法分析所使用的 Token 流 */
    private final TokenStream tokens;

    /**
     * 使用词法分析得到的 Token 列表构造上下文。
     *
     * @param tokens 词法分析器生成的 Token 集合
     */
    public ParserContext(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * 获取封装的 Token 流，用于驱动语法分析过程。
     *
     * @return 当前使用的 {@link TokenStream} 实例
     */
    public TokenStream getTokens() {
        return tokens;
    }
}