package org.jcnc.snow.compiler.parser.ast;

import org.jcnc.snow.compiler.parser.ast.base.Node;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;

/**
 * {@code ImportNode} 表示抽象语法树（AST）中的 import 语句节点。
 * <p>
 * import 语句用于引入外部模块或库文件，其语法形式一般为: 
 * {@code import my.module;}
 * </p>
 * <p>
 * 本节点仅存储导入目标模块的名称，不包含路径解析或绑定逻辑，
 * 这些通常由语义分析器或模块加载器处理。
 * </p>
 *
 * @param moduleName 被导入的模块名称，通常为点分层次结构（如 "core.utils"）
 * @param context    节点上下文信息（包含行号、列号等）
 */
public record ImportNode(
        String moduleName,
        NodeContext context
) implements Node {
}
