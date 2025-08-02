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

    /**
     * 前缀解析器注册表（按 Token 类型名或词素索引）。
     * <p>
     * 用于存储所有支持的前缀表达式解析器，例如字面量、变量、分组、数组、一元运算等。
     * 支持通过 TokenType 的名称和特定词素（如 "(", "["）两种方式索引。
     * </p>
     */
    private static final Map<String, PrefixParselet> prefixes = new HashMap<>();

    /**
     * 中缀解析器注册表（按运算符词素索引）。
     * <p>
     * 用于存储所有支持的中缀表达式解析器，如二元运算、函数调用、下标、成员访问等。
     * 仅通过词素索引（如 "+", "-", "(", "[" 等）。
     * </p>
     */
    private static final Map<String, InfixParselet> infixes = new HashMap<>();

    static {
        // -------- 前缀解析器注册 --------
        // 注册数字字面量解析
        prefixes.put(TokenType.NUMBER_LITERAL.name(),   new NumberLiteralParselet());
        // 注册标识符（变量名）解析
        prefixes.put(TokenType.IDENTIFIER.name(),       new IdentifierParselet());
        // 注册字符串字面量解析
        prefixes.put(TokenType.STRING_LITERAL.name(),   new StringLiteralParselet());
        // 注册布尔字面量解析
        prefixes.put(TokenType.BOOL_LITERAL.name(),     new BoolLiteralParselet());

        // 支持括号分组、数组字面量
        prefixes.put(TokenType.LPAREN.name(),           new GroupingParselet());
        prefixes.put(TokenType.LBRACKET.name(),         new ArrayLiteralParselet());
        // 兼容直接以词素注册（如 '(' '['）
        prefixes.put("(", new GroupingParselet());
        prefixes.put("[", new ArrayLiteralParselet());

        // 一元前缀运算符（负号、逻辑非）
        prefixes.put(TokenType.MINUS.name(), new UnaryOperatorParselet());
        prefixes.put(TokenType.NOT.name(),   new UnaryOperatorParselet());
        prefixes.put("-", new UnaryOperatorParselet());
        prefixes.put("!", new UnaryOperatorParselet());

        // -------- 中缀解析器注册 --------
        // 注册常见二元运算符（加减乘除、取模）
        infixes.put("+",  new BinaryOperatorParselet(Precedence.SUM,     true));
        infixes.put("-",  new BinaryOperatorParselet(Precedence.SUM,     true));
        infixes.put("*",  new BinaryOperatorParselet(Precedence.PRODUCT, true));
        infixes.put("/",  new BinaryOperatorParselet(Precedence.PRODUCT, true));
        infixes.put("%",  new BinaryOperatorParselet(Precedence.PRODUCT, true));
        // 比较运算符
        infixes.put(">",  new BinaryOperatorParselet(Precedence.COMPARISON, true));
        infixes.put("<",  new BinaryOperatorParselet(Precedence.COMPARISON, true));
        infixes.put(">=", new BinaryOperatorParselet(Precedence.COMPARISON, true));
        infixes.put("<=", new BinaryOperatorParselet(Precedence.COMPARISON, true));
        // 相等性
        infixes.put("==", new BinaryOperatorParselet(Precedence.EQUALITY, true));
        infixes.put("!=", new BinaryOperatorParselet(Precedence.EQUALITY, true));
        // 逻辑与或
        infixes.put("&&", new BinaryOperatorParselet(Precedence.AND, true));
        infixes.put("||", new BinaryOperatorParselet(Precedence.OR, true));
        // 调用、索引、成员访问
        infixes.put("(",  new CallParselet());
        infixes.put("[",  new IndexParselet());
        infixes.put(".",  new MemberParselet());
    }

    /**
     * 解析任意表达式的统一入口。
     * <p>
     * 该方法将以最低优先级启动表达式递归解析，能够自动适配和处理多层嵌套或复杂组合表达式。
     * </p>
     *
     * @param ctx 当前解析上下文对象（持有 token 流等信息）
     * @return 解析得到的表达式 AST 节点对象
     */
    @Override
    public ExpressionNode parse(ParserContext ctx) {
        return parseExpression(ctx, Precedence.LOWEST);
    }

    /**
     * 按指定优先级解析表达式（Pratt 算法核心）。
     * <p>
     * 1. 先取当前 token，查找对应的前缀解析器进行初始解析，构建表达式左侧（如字面量、变量等）。
     * 2. 然后循环检测是否有更高优先级的中缀操作符，
     *    若有则递归处理右侧表达式并组合为新的表达式节点。
     * </p>
     * <p>
     * 未找到对应前缀或中缀解析器时会抛出 {@link UnsupportedFeature} 异常。
     * </p>
     *
     * @param ctx  解析上下文
     * @param prec 当前运算符优先级（用于控制递归层级）
     * @return 解析构建好的表达式节点
     * @throws UnsupportedFeature 遇到未注册的解析器时抛出
     */
    ExpressionNode parseExpression(ParserContext ctx, Precedence prec) {
        // 取下一个 token 作为本轮前缀表达式起始
        Token token = ctx.getTokens().next();

        // 查找前缀解析器（先按类型名，再按词素）
        PrefixParselet prefix = prefixes.get(token.getType().name());
        if (prefix == null) {
            prefix = prefixes.get(token.getLexeme());
        }
        if (prefix == null) {
            // 未找到前缀解析器则报错
            throw new UnsupportedFeature(
                    "没有为该 Token 类型注册前缀解析器: " + token.getType(),
                    token.getLine(),
                    token.getCol()
            );
        }

        // 执行前缀解析，获得左侧表达式
        ExpressionNode left = prefix.parse(ctx, token);

        // 不断尝试查找优先级更高的中缀运算符，递归处理表达式链
        while (!ctx.getTokens().isAtEnd()
                && prec.ordinal() < nextPrecedence(ctx)) {
            // 查看下一个 token 词素，查找中缀解析器
            String lex = ctx.getTokens().peek().getLexeme();
            InfixParselet infix = infixes.get(lex);
            if (infix == null) {
                // 若未注册中缀解析器，则直接抛异常（常见于语法错误）
                Token t = ctx.getTokens().peek();
                throw new UnsupportedFeature(
                        "没有为该运算符注册中缀解析器: '" + lex + "'",
                        t.getLine(),
                        t.getCol()
                );
            }
            // 使用中缀解析器处理表达式组合
            left = infix.parse(ctx, left);
        }
        // 返回本层递归已解析的表达式节点
        return left;
    }

    /**
     * 获取下一个 token 词素对应的中缀运算符优先级（Pratt 算法关键）。
     * <p>
     * 用于决定当前是否需要递归处理更高优先级的中缀操作。
     * </p>
     *
     * @param ctx 当前解析上下文
     * @return 下一个中缀运算符的优先级序号；若无注册解析器则返回 -1
     */
    private int nextPrecedence(ParserContext ctx) {
        InfixParselet infix = infixes.get(ctx.getTokens().peek().getLexeme());
        return infix != null ? infix.precedence().ordinal() : -1;
    }
}
