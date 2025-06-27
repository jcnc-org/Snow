package org.jcnc.snow.compiler.parser.ast;

import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;

import java.util.Optional;

/**
 * {@code ReturnNode} 表示抽象语法树（AST）中的 return 语句节点。
 * <p>
 * return 语句用于从当前函数中返回控制权，并可携带一个可选的返回值表达式。
 * </p>
 * <p>
 * 示例：
 * <ul>
 *   <li>{@code return;}</li>
 *   <li>{@code return x + 1;}</li>
 * </ul>
 * </p>
 */
public class ReturnNode implements StatementNode {

    /** 可选的返回值表达式 */
    private final Optional<ExpressionNode> expression;

    /** 当前节点所在的行号 **/
    private final int line;

    /** 当前节点所在的列号 **/
    private final int column;

    /** 当前节点所在的文件 **/
    private final String file;

    /**
     * 构造一个 {@code ReturnNode} 实例。
     *
     * @param expression 返回值表达式，如果无返回值则可为 {@code null}
     */
    public ReturnNode(ExpressionNode expression, int line, int column, String file) {
        this.expression = Optional.ofNullable(expression);
        this.line = line;
        this.column = column;
        this.file = file;
    }

    /**
     * 获取可选的返回值表达式。
     *
     * @return 如果有返回值则返回 {@code Optional.of(expression)}，否则返回 {@code Optional.empty()}
     */
    public Optional<ExpressionNode> getExpression() {
        return expression;
    }

    /**
     * 获取当前表达式所在的行号。
     *
     * @return 当前表达式的行号。
     */
    public int line() {
        return line;
    }

    /**
     * 获取当前表达式所在的列号。
     *
     * @return 当前表达式的列号。
     */
    public int column() {
        return column;
    }

    /**
     * 获取当前表达式所在的文件名。
     *
     * @return 当前表达式所在的文件名。
     */
    public String file()   { return file;   }
}