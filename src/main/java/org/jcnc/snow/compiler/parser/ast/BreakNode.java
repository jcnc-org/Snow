package org.jcnc.snow.compiler.parser.ast;

import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;

/**
 * {@code BreakNode} 表示循环体中的 break 语句。
 * 出现时应立即终止当前（最内层）循环。
 */
public record BreakNode(NodeContext context) implements StatementNode {

    @Override
    public String toString() {
        return "break@" + context;
    }
}
