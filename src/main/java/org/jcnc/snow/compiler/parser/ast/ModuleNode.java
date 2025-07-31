package org.jcnc.snow.compiler.parser.ast;

import org.jcnc.snow.compiler.parser.ast.base.Node;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;

import java.util.List;
import java.util.StringJoiner;

/**
 * 表示模块定义的 AST 节点。
 * 一个模块通常由模块名、导入语句列表和函数定义列表组成。
 *
 * @param name      模块名称。
 * @param imports   模块导入列表，每个导入是一个 {@link ImportNode}。
 * @param functions 模块中的函数列表，每个函数是一个 {@link FunctionNode}。
 * @param context   节点上下文信息（包含行号、列号等）
 */
public record ModuleNode(
        String name,
        List<ImportNode> imports,
        List<DeclarationNode> globals,
        List<FunctionNode> functions,
        NodeContext context
) implements Node {

    /**
     * 返回模块节点的字符串表示形式，包含模块名、导入模块列表和函数列表。
     *
     * @return 模块的简洁字符串表示，用于调试和日志输出。
     */
    @Override
    public String toString() {
        StringJoiner importJoiner = new StringJoiner(", ");
        for (ImportNode imp : imports) {
            importJoiner.add(imp.moduleName());
        }
        StringJoiner globalJoiner = new StringJoiner(", ");
        for (DeclarationNode g : globals) {
            globalJoiner.add(g.getType() + " " + g.getName());
        }
        StringJoiner funcJoiner = new StringJoiner(", ");
        for (FunctionNode fn : functions) {
            funcJoiner.add(fn.name());
        }
        return "Module(name=" + name
                + ", imports=[" + importJoiner + "]"
                + ", globals=[" + globalJoiner + "]"
                + ", functions=[" + funcJoiner + "])";
    }
}
