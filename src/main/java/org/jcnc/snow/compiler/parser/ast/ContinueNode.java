package org.jcnc.snow.compiler.parser.ast;

import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;

/**
 * {@code ContinueNode} 表示循环体中的 continue 语句节点。
 * <p>
 * continue 语句用于跳过当前循环剩余部分，直接进入下一次循环的 step→cond 阶段。
 * 该节点仅作为语法树中的一种语句类型出现。
 * </p>
 */
public record ContinueNode(NodeContext context) implements StatementNode {

    @Override
    public String toString() {
        return "continue";
    }
}
