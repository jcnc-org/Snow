package org.jcnc.snow.compiler.parser.expression;

import org.jcnc.snow.compiler.parser.ast.CallExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.expression.base.InfixParselet;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code CallParselet} 表示函数调用语法的中缀解析器。
 * <p>
 * 用于处理形如 {@code foo(arg1, arg2)} 的函数调用结构。
 * 在 Pratt 解析器架构中，该解析器在函数名之后接收括号开始的调用参数，
 * 构建 {@link CallExpressionNode} 抽象语法树节点。
 * </p>
 */
public class CallParselet implements InfixParselet {

    /**
     * 解析函数调用表达式，格式为 {@code callee(args...)}。
     *
     * @param ctx  当前解析上下文
     * @param left 函数名或调用目标（即 callee 表达式）
     * @return 构建完成的函数调用 AST 节点
     */
    @Override
    public ExpressionNode parse(ParserContext ctx, ExpressionNode left) {
        ctx.getTokens().next(); // 消费 "("

        List<ExpressionNode> args = new ArrayList<>();

        if (!ctx.getTokens().peek().getLexeme().equals(")")) {
            do {
                args.add(new PrattExpressionParser().parse(ctx));
            } while (ctx.getTokens().match(","));
        }

        ctx.getTokens().expect(")"); // 消费并验证 ")"
        return new CallExpressionNode(left, args);
    }

    /**
     * 获取函数调用操作的优先级。
     *
     * @return 表达式优先级 {@link Precedence#CALL}
     */
    @Override
    public Precedence precedence() {
        return Precedence.CALL;
    }
}
