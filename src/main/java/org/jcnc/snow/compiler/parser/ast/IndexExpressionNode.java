package org.jcnc.snow.compiler.parser.ast;

import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;

/**
 * {@code IndexExpressionNode} 表示数组/集合的下标访问表达式节点，例如 {@code arr[i]}。
 * <p>
 * 支持多维数组嵌套（如 arr[i][j]）。
 * </p>
 *
 * @param array   被访问的目标表达式（通常是数组、集合或嵌套下标表达式）
 * @param index   下标表达式（必须为数值类型）
 * @param context 源码上下文信息（如位置）
 */
public record IndexExpressionNode(
        /*
          下标访问的目标表达式（如 arr）。
         */
        ExpressionNode array,

        /*
          下标表达式（如 i）。
         */
        ExpressionNode index,

        /*
          源码上下文信息（如行号、文件名等）。
         */
        NodeContext context
) implements ExpressionNode {

    /**
     * 返回形如 "arr[i]" 的字符串表示。
     *
     * @return 表达式的字符串形式
     */
    @Override
    public String toString() {
        return array + "[" + index + "]";
    }
}
