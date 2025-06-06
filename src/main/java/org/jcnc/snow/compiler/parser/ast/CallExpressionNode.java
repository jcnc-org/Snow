package org.jcnc.snow.compiler.parser.ast;

import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;

import java.util.List;

/**
 * {@code CallExpressionNode} 表示抽象语法树（AST）中的函数调用表达式节点。
 * <p>
 * 函数调用表达式用于表示函数或过程的调用操作，
 * 包括被调用对象（callee）以及一组参数表达式（arguments）。
 * </p>
 *
 * @param callee    被调用的表达式节点，通常为函数标识符或成员访问表达式。
 * @param arguments 参数表达式列表，依照调用顺序排列。
 */
public record CallExpressionNode(ExpressionNode callee, List<ExpressionNode> arguments) implements ExpressionNode {

    /**
     * 返回函数调用表达式的字符串形式。
     * <p>
     * 该格式将输出为类似 {@code foo(a, b, c)} 的形式，
     * 便于调试与语法树可视化。
     * </p>
     *
     * @return 表示函数调用的字符串表示
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(callee).append("(");
        for (int i = 0; i < arguments.size(); i++) {
            sb.append(arguments.get(i));
            if (i + 1 < arguments.size()) sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }
}
