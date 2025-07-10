package org.jcnc.snow.compiler.parser.ast.base;

/**
 * NodeContext 记录 AST 节点的位置信息（文件、行、列）。
 */
public record NodeContext(int line, int column, String file) {
    @Override
    public String toString() {
        return file + ":" + line + ":" + column;
    }
}
