package org.jcnc.snow.compiler.semantic.error;

import org.jcnc.snow.compiler.parser.ast.base.Node;

/**
 * 表示一次语义错误（Semantic Error）。
 * <p>
 * 本类用于在语义分析阶段记录出错的 AST 节点及对应的错误信息，<br>
 * 便于后续错误报告、调试和 IDE 集成等多种用途。
 * </p>
 * <ul>
 *   <li>通过关联的 {@link Node} 提供出错的具体位置（文件、行号、列号等）信息；</li>
 *   <li>支持格式化错误输出，友好展示错误发生的上下文；</li>
 *   <li>避免直接输出 AST 节点的默认 <code>toString()</code> 形式。</li>
 * </ul>
 *
 * <p><b>示例输出：</b></p>
 * <pre>
 *   playground\main.snow: 行 7, 列 28: 参数类型不匹配 (位置 1): 期望 int, 实际 long
 * </pre>
 *
 * @param node    指向发生语义错误的 AST 节点，可用于获取详细的位置信息（文件名、行号、列号等）
 * @param message 描述该语义错误的详细信息，通常为人类可读的解释或修正建议
 *
 */
public record SemanticError(Node node, String message) {

    /**
     * 返回该语义错误的字符串描述，格式如下：
     * <pre>
     * [文件名: ]行 X, 列 Y: [错误信息]
     * </pre>
     * 若节点未能提供有效位置，则输出“未知位置”。
     *
     * @return 适合用户阅读的语义错误描述字符串
     */
    @Override
    public String toString() {
        // Node 假定提供 line() / column() 方法；如无则返回 -1
        int line = -1;
        int col = -1;
        String file = null;

        if (node != null) {
            try {
                line = (int) node.getClass().getMethod("line").invoke(node);
            } catch (Exception ignored) {
            }
            try {
                col = (int) node.getClass().getMethod("column").invoke(node);
            } catch (Exception ignored) {
            }
            try {
                file = (String) node.getClass().getMethod("file").invoke(node);
            } catch (Exception ignored) {
            }
        }

        StringBuilder sb = new StringBuilder();
        if (file != null && !file.isBlank()) sb.append(file).append(": ");
        sb.append((line >= 0 && col >= 0) ? "行 " + line + ", 列 " + col : "未知位置");
        sb.append(": ").append(message);
        return sb.toString();
    }
}
