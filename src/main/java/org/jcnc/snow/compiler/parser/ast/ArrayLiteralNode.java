package org.jcnc.snow.compiler.parser.ast;

import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;

import java.util.List;

/**
 * {@code ArrayLiteralNode} 表示数组字面量表达式节点。
 * <p>
 * 例如：[1, 2, 3] 或 [[1, 2], [3, 4]] 这样的语法结构，均对应本节点类型。
 * </p>
 *
 * <ul>
 *     <li>{@link #elements()} 保存所有元素表达式节点。</li>
 *     <li>{@link #context()} 表示该节点在源代码中的上下文信息（如位置、父节点等）。</li>
 * </ul>
 */
public record ArrayLiteralNode(
        /*
          数组字面量中的所有元素表达式（按顺序）。
         */
        List<ExpressionNode> elements,

        /*
          节点的上下文信息（如源码位置等）。
         */
        NodeContext context
) implements ExpressionNode {

    /**
     * 返回字符串形式，如 {@code Array[1, 2, 3]}。
     *
     * @return 表示该数组字面量节点的字符串
     */
    @Override
    public String toString() {
        return "Array" + elements;
    }
}
