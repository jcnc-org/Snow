package org.jcnc.snow.compiler.parser.expression;

import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.parser.ast.BoolLiteralNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.expression.base.PrefixParselet;

/**
 * {@code BoolLiteralParselet} 是用于解析布尔字面量的前缀解析子（prefix parselet）。
 * <p>
 * 本类实现了 {@link PrefixParselet} 接口，用于在语法分析阶段将布尔类型的词法单元（如 "true" 或 "false"）
 * 转换为相应的抽象语法树（AST）节点 {@link BoolLiteralNode}。
 */
public class BoolLiteralParselet implements PrefixParselet {

    /**
     * 解析布尔字面量的词法单元，并返回对应的布尔字面量节点。
     * <p>
     * 本方法被语法分析器在遇到布尔字面量词法单元时调用。
     * 它将词法单元的词素（lexeme）传递给 {@link BoolLiteralNode} 构造器，
     * 并返回构造得到的 AST 节点。
     * </p>
     *
     * @param ctx   当前的语法分析上下文，用于提供所需的解析信息
     * @param token 代表布尔字面量的词法单元
     * @return 对应的 {@link BoolLiteralNode} 实例
     */
    @Override
    public ExpressionNode parse(ParserContext ctx, Token token) {
        return new BoolLiteralNode(token.getLexeme(), token.getLine(), token.getCol(), ctx.getSourceName());
    }
}
