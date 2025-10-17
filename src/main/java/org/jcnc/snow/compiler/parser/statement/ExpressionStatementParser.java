package org.jcnc.snow.compiler.parser.statement;

import org.jcnc.snow.compiler.lexer.token.TokenType;
import org.jcnc.snow.compiler.parser.ast.AssignmentNode;
import org.jcnc.snow.compiler.parser.ast.ExpressionStatementNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;
import org.jcnc.snow.compiler.parser.context.ParserContext;
import org.jcnc.snow.compiler.parser.context.TokenStream;
import org.jcnc.snow.compiler.parser.context.UnexpectedToken;
import org.jcnc.snow.compiler.parser.expression.PrattExpressionParser;

/**
 * {@code ExpressionStatementParser} 用于解析通用表达式语句（赋值或普通表达式）。
 * <p>
 * 支持以下两种语法结构:
 * <pre>{@code
 * x = 1 + 2        // 赋值语句
 * doSomething()    // 一般表达式语句
 * this.name = n    // 将 this.name 赋值语法糖为对 name 的赋值
 * }</pre>
 * <p>
 * - 以标识符开头且后接 '=' 时，解析为 {@link AssignmentNode}。
 * - 否则先解析为一般表达式；若后续遇到 '='，则回退为“<expr> = <expr>”赋值语句。
 * - 所有表达式语句必须以换行符（NEWLINE）结尾。
 * <p>
 */
public class ExpressionStatementParser implements StatementParser {

    /**
     * 解析单行表达式语句，根据上下文判断其为赋值语句或普通表达式语句。
     *
     * @param ctx 当前解析上下文，提供词法流与环境信息
     * @return {@link AssignmentNode} 或 {@link ExpressionStatementNode} 语法节点
     * @throws UnexpectedToken 若遇到非法起始（关键字 'end' 等）
     */
    @Override
    public StatementNode parse(ParserContext ctx) {
        TokenStream ts = ctx.getTokens();

        // ----------- 起始 token 合法性检查（放宽以支持 this 开头）-----------
        if (ts.peek().getType() == TokenType.NEWLINE) {
            // 空行不应进入表达式解析，直接抛出异常
            throw new UnexpectedToken(
                    "无法解析以空行开头的表达式",
                    ts.peek().getLine(),
                    ts.peek().getCol()
            );
        }
        if (ts.peek().getType() == TokenType.KEYWORD) {
            String kw = ts.peek().getLexeme();
            // 仅允许 this/super 作为表达式起始；其它关键字（如 end/if/else 等）仍禁止
            if (!"this".equals(kw) && !"super".equals(kw)) {
                throw new UnexpectedToken(
                        "无法解析以关键字开头的表达式: " + kw,
                        ts.peek().getLine(),
                        ts.peek().getCol()
                );
            }
        }

        int line = ts.peek().getLine();
        int column = ts.peek().getCol();
        String file = ctx.getSourceName();

        // ------------- 简单形式: IDENTIFIER = expr -------------
        // 快速路径：如 "a = ..."，直接识别为赋值语句，无需完整表达式树回退
        if (ts.peek().getType() == TokenType.IDENTIFIER && "=".equals(ts.peek(1).getLexeme())) {
            String varName = ts.next().getLexeme();    // 消费 IDENTIFIER
            ts.expect("=");                            // 消费 '='
            ExpressionNode value = new PrattExpressionParser().parse(ctx); // 解析右侧表达式
            ts.expectType(TokenType.NEWLINE);
            // 返回简单变量赋值节点
            return new AssignmentNode(varName, value, new NodeContext(line, column, file));
        }

        // ------------- 通用形式: <expr> [= <expr>] -------------
        // 先解析潜在“左值表达式”或普通表达式
        ExpressionNode lhs = new PrattExpressionParser().parse(ctx);

        // 若遇到等号，则尝试回退为赋值语句（兼容更复杂的左值表达式）
        if ("=".equals(ts.peek().getLexeme())) {
            ts.next(); // 消费 '='
            ExpressionNode rhs = new PrattExpressionParser().parse(ctx); // 解析右值表达式
            ts.expectType(TokenType.NEWLINE);

            // 根据左值 AST 类型，生成不同赋值节点
            if (lhs instanceof org.jcnc.snow.compiler.parser.ast.IdentifierNode id) {
                // 变量名赋值：a = rhs
                return new AssignmentNode(id.name(), rhs, new NodeContext(line, column, file));

            } else if (lhs instanceof org.jcnc.snow.compiler.parser.ast.IndexExpressionNode idx) {
                // 下标赋值：a[i] = rhs
                return new org.jcnc.snow.compiler.parser.ast.IndexAssignmentNode(idx, rhs, new NodeContext(line, column, file));

            } else if (lhs instanceof org.jcnc.snow.compiler.parser.ast.MemberExpressionNode mem
                    && mem.object() instanceof org.jcnc.snow.compiler.parser.ast.IdentifierNode oid
                    && "this".equals(oid.name())) {
                // 支持：this.field = rhs
                // 语法糖：降级为对当前作用域同名变量的赋值，相当于 "field = rhs"
                return new AssignmentNode(mem.member(), rhs, new NodeContext(line, column, file));

            } else {
                // 其它成员赋值（如 a.b = ...）不支持，报错
                throw new UnexpectedToken("不支持的赋值左值类型: " + lhs.getClass().getSimpleName(), line, column);
            }
        }

        // 不是赋值，则当作普通表达式语句处理
        ts.expectType(TokenType.NEWLINE);
        return new ExpressionStatementNode(lhs, new NodeContext(line, column, file));
    }
}
