package org.jcnc.snow.compiler.parser.expression;

import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.lexer.token.TokenType;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.context.UnsupportedFeature;
import org.jcnc.snow.compiler.parser.expression.base.ExpressionParser;
import org.jcnc.snow.compiler.parser.expression.base.InfixParselet;
import org.jcnc.snow.compiler.parser.expression.base.PrefixParselet;

import java.util.HashMap;
import java.util.Map;

/**
 * {@code PrattExpressionParser} 基于 Pratt 算法的表达式解析器实现。
 * <p>
 * 该类通过前缀（PrefixParselet）和中缀（InfixParselet）解析器注册表，
 * 支持灵活扩展的表达式语法，包括字面量、变量、函数调用、成员访问和各种运算符表达式。
 * </p>
 * <p>
 * 运算符优先级通过枚举控制，结合递归解析实现高效的优先级处理和语法结构解析。
 * 未注册的语法类型或运算符会统一抛出 {@link UnsupportedFeature} 异常。
 * </p>
 */
public class PrattExpressionParser implements ExpressionParser {

    /** 前缀解析器注册表（按 Token 类型名索引） */
    private static final Map<String, PrefixParselet> prefixes = new HashMap<>();
    /** 中缀解析器注册表（按运算符词素索引） */
    private static final Map<String, InfixParselet> infixes = new HashMap<>();

    static {
        // 前缀解析器注册
        prefixes.put(TokenType.NUMBER_LITERAL.name(),   new NumberLiteralParselet());
        prefixes.put(TokenType.IDENTIFIER.name(),       new IdentifierParselet());
        prefixes.put(TokenType.LPAREN.name(),           new GroupingParselet());
        prefixes.put(TokenType.STRING_LITERAL.name(),   new StringLiteralParselet());
        prefixes.put(TokenType.BOOL_LITERAL.name(),     new BoolLiteralParselet());

        // 一元前缀运算符
        prefixes.put(TokenType.MINUS.name(), new UnaryOperatorParselet());
        prefixes.put(TokenType.NOT.name(),   new UnaryOperatorParselet());

        // 中缀解析器注册
        infixes.put("+",  new BinaryOperatorParselet(Precedence.SUM,     true));
        infixes.put("-",  new BinaryOperatorParselet(Precedence.SUM,     true));
        infixes.put("*",  new BinaryOperatorParselet(Precedence.PRODUCT, true));
        infixes.put("/",  new BinaryOperatorParselet(Precedence.PRODUCT, true));
        infixes.put("%",  new BinaryOperatorParselet(Precedence.PRODUCT, true));
        infixes.put(">",  new BinaryOperatorParselet(Precedence.SUM,     true));
        infixes.put("<",  new BinaryOperatorParselet(Precedence.SUM,     true));
        infixes.put("==", new BinaryOperatorParselet(Precedence.SUM,     true));
        infixes.put("!=", new BinaryOperatorParselet(Precedence.SUM,     true));
        infixes.put(">=", new BinaryOperatorParselet(Precedence.SUM,     true));
        infixes.put("<=", new BinaryOperatorParselet(Precedence.SUM,     true));
        infixes.put("(",  new CallParselet());
        infixes.put(".",  new MemberParselet());
    }

    /**
     * 表达式解析统一入口。
     * 以最低优先级启动递归下降，适配任意表达式复杂度。
     *
     * @param ctx 当前解析上下文
     * @return 解析后的表达式 AST 节点
     */
    @Override
    public ExpressionNode parse(ParserContext ctx) {
        return parseExpression(ctx, Precedence.LOWEST);
    }

    /**
     * 按指定优先级解析表达式。Pratt 算法主循环。
     * <p>
     * 先根据当前 Token 类型查找前缀解析器进行初始解析，
     * 然后根据优先级不断递归处理中缀运算符和右侧表达式。
     * </p>
     *
     * @param ctx  解析上下文
     * @param prec 当前运算符优先级阈值
     * @return 构建完成的表达式节点
     * @throws UnsupportedFeature 若遇到未注册的前缀或中缀解析器
     */
    ExpressionNode parseExpression(ParserContext ctx, Precedence prec) {
        Token token = ctx.getTokens().next();
        PrefixParselet prefix = prefixes.get(token.getType().name());
        if (prefix == null) {
            throw new UnsupportedFeature(
                    "没有为该 Token 类型注册前缀解析器: " + token.getType(),
                    token.getLine(),
                    token.getCol()
            );
        }

        ExpressionNode left = prefix.parse(ctx, token);

        while (!ctx.getTokens().isAtEnd()
                && prec.ordinal() < nextPrecedence(ctx)) {
            String lex = ctx.getTokens().peek().getLexeme();
            InfixParselet infix = infixes.get(lex);
            if (infix == null) {
                Token t = ctx.getTokens().peek();
                throw new UnsupportedFeature(
                        "没有为该运算符注册中缀解析器: '" + lex + "'",
                        t.getLine(),
                        t.getCol()
                );
            }
            left = infix.parse(ctx, left);
        }
        return left;
    }

    /**
     * 获取下一个中缀解析器的优先级（Pratt 算法核心）。
     *
     * @param ctx 当前解析上下文
     * @return 下一个中缀运算符的优先级序号；若无解析器则为 -1
     */
    private int nextPrecedence(ParserContext ctx) {
        InfixParselet infix = infixes.get(ctx.getTokens().peek().getLexeme());
        return infix != null ? infix.precedence().ordinal() : -1;
    }
}
