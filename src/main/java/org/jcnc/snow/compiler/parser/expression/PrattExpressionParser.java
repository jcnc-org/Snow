package org.jcnc.snow.compiler.parser.expression;

import org.jcnc.snow.compiler.lexer.token.Token;
import org.jcnc.snow.compiler.lexer.token.TokenType;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.expression.base.ExpressionParser;
import org.jcnc.snow.compiler.parser.expression.base.InfixParselet;
import org.jcnc.snow.compiler.parser.expression.base.PrefixParselet;

import java.util.HashMap;
import java.util.Map;

/**
 * {@code PrattExpressionParser} 是基于 Pratt 算法实现的表达式解析器。
 * <p>
 * 它支持灵活的运算符优先级控制，结合前缀（PrefixParselet）和中缀（InfixParselet）解析器，
 * 可高效解析复杂表达式结构，包括：
 * <ul>
 *     <li>字面量（数字、字符串）</li>
 *     <li>标识符</li>
 *     <li>函数调用、成员访问</li>
 *     <li>带括号的表达式、二元运算符</li>
 * </ul>
 * 本类提供统一注册机制和递归表达式解析入口。
 * </p>
 */
public class PrattExpressionParser implements ExpressionParser {

    /**
     * 前缀解析器注册表：按 Token 类型映射
     */
    private static final Map<String, PrefixParselet> prefixes = new HashMap<>();

    /**
     * 中缀解析器注册表：按运算符词素映射
     */
    private static final Map<String, InfixParselet> infixes = new HashMap<>();

    static {
        // 注册前缀解析器
        prefixes.put(TokenType.NUMBER_LITERAL.name(), new NumberLiteralParselet());
        prefixes.put(TokenType.IDENTIFIER.name(), new IdentifierParselet());
        prefixes.put(TokenType.LPAREN.name(), new GroupingParselet());
        prefixes.put(TokenType.STRING_LITERAL.name(), new StringLiteralParselet());
        prefixes.put(TokenType.BOOL_LITERAL.name(), new BoolLiteralParselet());

        // 注册一元前缀运算
        prefixes.put(TokenType.MINUS.name(), new UnaryOperatorParselet());
        prefixes.put(TokenType.NOT.name(), new UnaryOperatorParselet());

        // 注册中缀解析器
        infixes.put("+", new BinaryOperatorParselet(Precedence.SUM, true));
        infixes.put("-", new BinaryOperatorParselet(Precedence.SUM, true));
        infixes.put("*", new BinaryOperatorParselet(Precedence.PRODUCT, true));
        infixes.put("/", new BinaryOperatorParselet(Precedence.PRODUCT, true));
        infixes.put("%", new BinaryOperatorParselet(Precedence.PRODUCT, true));
        infixes.put(">", new BinaryOperatorParselet(Precedence.SUM, true));
        infixes.put("<", new BinaryOperatorParselet(Precedence.SUM, true));
        infixes.put("==", new BinaryOperatorParselet(Precedence.SUM, true));
        infixes.put("!=", new BinaryOperatorParselet(Precedence.SUM, true));
        infixes.put(">=", new BinaryOperatorParselet(Precedence.SUM, true));
        infixes.put("<=", new BinaryOperatorParselet(Precedence.SUM, true));
        infixes.put("(", new CallParselet());
        infixes.put(".", new MemberParselet());
    }

    /**
     * 表达式解析入口，使用最低优先级启动递归解析。
     *
     * @param ctx 当前语法解析上下文
     * @return 表达式抽象语法树节点
     */
    @Override
    public ExpressionNode parse(ParserContext ctx) {
        return parseExpression(ctx, Precedence.LOWEST);
    }

    /**
     * 根据指定优先级解析表达式。
     *
     * @param ctx  当前上下文
     * @param prec 当前优先级阈值
     * @return 构建完成的表达式节点
     */
    ExpressionNode parseExpression(ParserContext ctx, Precedence prec) {
        Token token = ctx.getTokens().next();
        PrefixParselet prefix = prefixes.get(token.getType().name());
        if (prefix == null) {
            throw new IllegalStateException("没有为该 Token 类型注册前缀解析器: " + token.getType());
        }

        ExpressionNode left = prefix.parse(ctx, token);

        while (ctx.getTokens().isAtEnd()
                && prec.ordinal() < nextPrecedence(ctx)) {
            String lex = ctx.getTokens().peek().getLexeme();
            InfixParselet infix = infixes.get(lex);
            if (infix == null) break;
            left = infix.parse(ctx, left);
        }
        return left;
    }

    /**
     * 获取下一个中缀解析器的优先级，用于判断是否继续解析。
     *
     * @param ctx 当前上下文
     * @return 优先级枚举 ordinal 值；若无解析器则为 -1
     */
    private int nextPrecedence(ParserContext ctx) {
        InfixParselet infix = infixes.get(ctx.getTokens().peek().getLexeme());
        return infix != null ? infix.precedence().ordinal() : -1;
    }
}
