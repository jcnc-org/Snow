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
 * {@code PrattExpressionParser}
 * <p>
 * 基于 Pratt 算法的表达式解析器（经典“运算符优先级”递归解析框架）。
 * <ul>
 *   <li>支持注册前缀和中缀解析器，灵活组合表达式语法。</li>
 *   <li>支持字面量、变量、函数调用、成员访问、对象创建、各类一元/二元/多元运算。</li>
 *   <li>可快速扩展新语法（只需注册新的 Parselet 即可）。</li>
 *   <li>出错时统一抛出 {@link UnsupportedFeature}，便于调试和错误提示。</li>
 * </ul>
 * </p>
 */
public class PrattExpressionParser implements ExpressionParser {

    /**
     * 前缀解析器注册表（通过 Token 类型名或词素作为索引）。
     * <ul>
     *   <li>如数字字面量、标识符、字符串、布尔值、new、分组、数组、一元运算等。</li>
     *   <li>支持同时用 TokenType 名称和具体词素（如 "("、"-"）注册。</li>
     * </ul>
     */
    private static final Map<String, PrefixParselet> prefixes = new HashMap<>();

    /**
     * 中缀解析器注册表（通过运算符词素索引）。
     * <ul>
     *   <li>如 + - * / % 及比较、逻辑、函数调用、下标、成员访问等。</li>
     *   <li>仅词素索引（如 "+"、"-"、"("、"."）</li>
     * </ul>
     */
    private static final Map<String, InfixParselet> infixes = new HashMap<>();

    static {
        // ----------------- 前缀解析器注册 -----------------
        // 各种字面量/标识符
        prefixes.put(TokenType.NUMBER_LITERAL.name(), new NumberLiteralParselet());
        prefixes.put(TokenType.IDENTIFIER.name(),     new IdentifierParselet());
        prefixes.put(TokenType.STRING_LITERAL.name(), new StringLiteralParselet());
        prefixes.put(TokenType.BOOL_LITERAL.name(),   new BoolLiteralParselet());

        // 分组与数组字面量（两种索引方式）
        prefixes.put(TokenType.LPAREN.name(),   new GroupingParselet());
        prefixes.put(TokenType.LBRACKET.name(), new ArrayLiteralParselet());
        prefixes.put("(", new GroupingParselet());
        prefixes.put("[", new ArrayLiteralParselet());

        // 一元前缀运算符（如负号、逻辑非），同样用两种方式注册
        prefixes.put(TokenType.MINUS.name(), new UnaryOperatorParselet());
        prefixes.put(TokenType.NOT.name(),   new UnaryOperatorParselet());
        prefixes.put("-", new UnaryOperatorParselet());
        prefixes.put("!", new UnaryOperatorParselet());

        // 对象创建 new TypeName(args...)
        prefixes.put("new", new NewObjectParselet());

        // ----------------- 中缀解析器注册 -----------------
        // 常见二元算数运算符
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
        // 相等性判断
        infixes.put("==", new BinaryOperatorParselet(Precedence.EQUALITY, true));
        infixes.put("!=", new BinaryOperatorParselet(Precedence.EQUALITY, true));
        // 逻辑运算
        infixes.put("&&", new BinaryOperatorParselet(Precedence.AND, true));
        infixes.put("||", new BinaryOperatorParselet(Precedence.OR,  true));
        // 函数调用、数组下标、成员访问
        infixes.put("(", new CallParselet());
        infixes.put("[", new IndexParselet());
        infixes.put(".", new MemberParselet());
    }

    /**
     * 统一表达式解析入口，自动以最低优先级递归解析整个表达式。
     * <p>
     * 能解析嵌套、复合等所有合法表达式结构。
     * </p>
     *
     * @param ctx 当前解析上下文对象（含 token 流等信息）
     * @return 解析得到的表达式 AST 节点对象
     */
    @Override
    public ExpressionNode parse(ParserContext ctx) {
        return parseExpression(ctx, Precedence.LOWEST);
    }

    /**
     * Pratt 算法主递归循环：按给定优先级递归解析表达式。
     * <p>
     * 实现按优先级吸收中缀操作符（如连续算术、链式调用、组合表达式等）。
     * </p>
     *
     * @param ctx  解析上下文
     * @param prec 当前已绑定优先级
     * @return 已解析的表达式节点
     */
    ExpressionNode parseExpression(ParserContext ctx, Precedence prec) {
        // 1) 消耗一个 token 作为前缀起点
        Token token = ctx.getTokens().next();

        // 2) 查找前缀解析器（优先按词素，再按 TokenType）
        PrefixParselet prefix = prefixes.get(token.getLexeme());
        if (prefix == null) {
            prefix = prefixes.get(token.getType().name());
        }
        if (prefix == null) {
            // 未注册前缀解析器，直接报错
            throw new UnsupportedFeature(
                    "没有为该 Token 注册前缀解析器: " + token.getLexeme() + " / " + token.getType(),
                    token.getLine(),
                    token.getCol()
            );
        }

        // 3) 前缀解析得到左侧表达式
        ExpressionNode left = prefix.parse(ctx, token);

        // 4) 主循环：不断吸收更高优先级的中缀操作，直到优先级不再提升
        while (!ctx.getTokens().isAtEnd() && prec.ordinal() < nextPrecedence(ctx)) {
            String lex = ctx.getTokens().peek().getLexeme();
            InfixParselet infix = infixes.get(lex);
            if (infix == null) {
                // nextPrecedence > prec 时一般已注册中缀解析器
                Token t = ctx.getTokens().peek();
                throw new UnsupportedFeature(
                        "没有为该运算符注册中缀解析器: '" + lex + "'",
                        t.getLine(),
                        t.getCol()
                );
            }
            // 递归组合更高优先级的中缀表达式
            left = infix.parse(ctx, left);
        }
        // 5) 返回本层解析完成的表达式节点
        return left;
    }

    /**
     * 获取下一个 token 对应的中缀运算符优先级（Pratt 算法关键）。
     * <p>
     * 若无注册的中缀解析器，则返回 -1。
     * </p>
     *
     * @param ctx 解析上下文
     * @return 下一个运算符优先级序号（无则-1）
     */
    private int nextPrecedence(ParserContext ctx) {
        InfixParselet infix = infixes.get(ctx.getTokens().peek().getLexeme());
        return infix != null ? infix.precedence().ordinal() : -1;
    }
}
