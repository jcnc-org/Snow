package org.jcnc.snow.compiler.parser.statement;

import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;

/**
 * {@code StatementParser} 是所有语句解析器的通用接口。
 * <p>
 * 其职责是从给定的 {@link ParserContext} 中读取并分析当前语句，构造并返回相应的抽象语法树节点。
 * 所有语句类型（如变量声明、赋值语句、控制结构、函数返回等）应提供对应的实现类。
 *
 * <p>
 * 通常，此接口的实现由 {@code StatementParserFactory} 根据当前关键字动态派发，用于解析模块体、
 * 条件分支、循环体或其他语句块中的单条语句。
 */
public interface StatementParser {

    /**
     * 解析一条语句，将其从词法表示转换为结构化语法树节点。
     *
     * @param ctx 当前的解析上下文，提供 token 流、状态与符号环境等。
     * @return 表示该语句的 AST 节点，类型为 {@link StatementNode} 或其子类。
     * @throws IllegalStateException 若语法非法或结构不完整。
     */
    StatementNode parse(ParserContext ctx);
}
