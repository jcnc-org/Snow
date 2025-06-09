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

    /** 当前语法分析所使用的资源文件名 */
    private final String sourceName;


    /**
     * 构造一个新的 {@code ParserContext} 实例，用于在语法分析阶段传递上下文信息。
     * <p>
     * 本构造方法接收词法分析得到的 {@link Token} 列表以及当前源文件名，<br>
     * 并将 Token 列表包装为 {@link TokenStream} 以便后续遍历与分析。<br>
     * 源文件名通常用于错误定位、调试和报错信息中指明具体文件。
     * </p>
     *
     * @param tokens     词法分析器生成的 Token 集合，表示待解析的完整源代码流
     * @param sourceName 当前正在解析的源文件名（或文件路径），用于错误报告和调试定位
     */
    public ParserContext(List<Token> tokens, String sourceName) {
        this.tokens = new TokenStream(tokens);
        this.sourceName = sourceName;
    }


    /**
     * 获取封装的 Token 流，用于驱动语法分析过程。
     *
     * @return 当前使用的 {@link TokenStream} 实例
     */
    public TokenStream getTokens() {
        return tokens;
    }

    /**
     * 获取资源文件名，用于发现错误后展示文件名。
     *
     * @return 当前语法分析所使用的资源文件名
     */
    public String getSourceName() {
        return sourceName;
    }
}