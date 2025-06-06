package org.jcnc.snow.compiler.parser.ast;

import org.jcnc.snow.compiler.parser.ast.base.Node;

import java.util.List;
import java.util.StringJoiner;

/**
 * 表示模块定义的 AST 节点。
 * 一个模块通常由模块名、导入语句列表和函数定义列表组成。
 * }
 *
 * @param name      模块名称。
 * @param imports   模块导入列表，每个导入是一个 {@link ImportNode}。
 * @param functions 模块中的函数列表，每个函数是一个 {@link FunctionNode}。
 */
public record ModuleNode(String name, List<ImportNode> imports, List<FunctionNode> functions) implements Node {

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
        StringJoiner funcJoiner = new StringJoiner(", ");
        for (FunctionNode fn : functions) {
            funcJoiner.add(fn.name());
        }
        return "Module(name=" + name
                + ", imports=[" + importJoiner + "]"
                + ", functions=[" + funcJoiner + "])";
    }
}
