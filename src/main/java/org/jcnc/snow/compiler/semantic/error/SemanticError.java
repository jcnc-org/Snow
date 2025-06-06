package org.jcnc.snow.compiler.semantic.error;


import org.jcnc.snow.compiler.parser.ast.base.Node;

/**
 * 表示一次语义错误。<br/>
 * <ul>
 *   <li>记录对应 {@link Node} 及出错信息；</li>
 *   <li>重写 {@link #toString()}，以 <code>行 X, 列 Y: message</code> 格式输出，</li>
 *   <li>避免默认的 <code>Node@hash</code> 形式。</li>
 * </ul>
 */
public record SemanticError(Node node, String message) {

    @Override
    public String toString() {
        // Node 假定提供 line() / column() 方法；如无则返回 -1
        int line = -1;
        int col = -1;
        if (node != null) {
            try {
                line = (int) node.getClass().getMethod("line").invoke(node);
                col = (int) node.getClass().getMethod("column").invoke(node);
            } catch (ReflectiveOperationException ignored) {
                // 若 Node 未提供 line/column 方法则保持 -1
            }
        }
        String pos = (line >= 0 && col >= 0) ? ("行 " + line + ", 列 " + col) : "未知位置";
        return pos + ": " + message;
    }
}
