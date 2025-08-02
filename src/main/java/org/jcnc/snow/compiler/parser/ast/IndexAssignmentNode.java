package org.jcnc.snow.compiler.parser.ast;

import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;

/**
 * {@code IndexAssignmentNode} 表示数组元素赋值语句节点，例如 {@code arr[i] = value}。
 * <p>
 * 对于多维数组，{@code target} 可以是嵌套的 {@link IndexExpressionNode}。
 * </p>
 *
 * @param target  要赋值的数组元素（支持多维数组下标）
 * @param value   右侧赋值表达式
 * @param context 该节点的源代码上下文信息
 */
public record IndexAssignmentNode(
        /*
          数组元素目标，支持多维数组下标（如 arr[i][j]）。
         */
        IndexExpressionNode target,

        /*
          被赋的右侧表达式
         */
        ExpressionNode value,

        /*
          节点的上下文信息（如源码位置等）
         */
        NodeContext context
) implements StatementNode {
    /**
     * 返回赋值语句的字符串表示（如 "arr[i] = value"）。
     *
     * @return 字符串形式
     */
    @Override
    public String toString() {
        return target + " = " + value;
    }
}
