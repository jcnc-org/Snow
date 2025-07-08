package org.jcnc.snow.compiler.parser.ast;

import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;

import java.util.List;

/**
 * {@code CallExpressionNode} 表示抽象语法树（AST）中的函数调用表达式节点。
 * <p>
 * 函数调用表达式用于表示函数或过程的调用操作，包含被调用对象（callee）以及一组参数表达式（arguments）。
 * </p>
 *
 * @param callee    被调用的表达式节点，通常为函数标识符或成员访问表达式，表示函数名或方法名等。
 * @param arguments 参数表达式列表，表示函数调用中传递给函数的实际参数。参数的顺序与调用顺序一致。
 * @param context   节点上下文信息（包含行号、列号等）。
 */
public record CallExpressionNode(
        ExpressionNode callee,           // 被调用的表达式节点，表示函数或方法名
        List<ExpressionNode> arguments,  // 函数调用的参数表达式列表
        NodeContext context              // 节点上下文信息（包含行号、列号等）
) implements ExpressionNode {

    /**
     * 返回函数调用表达式的字符串形式，便于调试与语法树可视化。
     * <p>
     * 该方法将表达式节点转化为类似 {@code foo(a, b, c)} 的格式，便于查看和理解抽象语法树的结构。
     * </p>
     *
     * @return 表示函数调用的字符串表示，格式为 {@code callee(arguments)}。
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(callee).append("(");  // 拼接函数名和左括号
        for (int i = 0; i < arguments.size(); i++) {
            sb.append(arguments.get(i));  // 拼接每个参数
            if (i + 1 < arguments.size()) sb.append(", ");  // 如果不是最后一个参数，添加逗号和空格
        }
        sb.append(")");  // 拼接右括号
        return sb.toString();  // 返回拼接好的字符串
    }
}
