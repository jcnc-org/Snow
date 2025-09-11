package org.jcnc.snow.compiler.parser.ast;

import org.jcnc.snow.compiler.parser.ast.base.Node;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;

import java.util.List;
import java.util.StringJoiner;

/**
 * {@code ModuleNode}
 * <p>
 * 抽象语法树（AST）顶层节点 —— 模块定义。
 * <ul>
 *   <li>代表一个完整的源码模块/文件（如 main, math 等）。</li>
 *   <li>包含模块名、导入列表、全局变量、结构体和函数定义等全部模块级内容。</li>
 *   <li>是整个编译流程的入口节点。</li>
 * </ul>
 * </p>
 *
 * @param name      模块名称
 * @param imports   导入模块列表
 * @param globals   全局变量/常量声明
 * @param structs   结构体定义列表
 * @param functions 函数定义列表
 * @param context   源代码位置信息
 */
public record ModuleNode(
        String name,
        List<ImportNode> imports,
        List<DeclarationNode> globals,
        List<StructNode> structs,
        List<FunctionNode> functions,
        NodeContext context
) implements Node {

    /**
     * 返回模块节点的简要字符串表示（用于日志、调试）。
     * 列出模块名、导入、全局变量、结构体、函数等简明内容。
     *
     * @return 字符串形式，如
     * Module(name=main, imports=[math], globals=[int x], structs=[Foo], functions=[bar])
     */
    @Override
    public String toString() {
        // 1) 导入模块列表字符串
        StringJoiner impJ = new StringJoiner(", ");
        imports.forEach(i -> impJ.add(i.moduleName()));

        // 2) 全局变量/常量列表字符串
        StringJoiner globJ = new StringJoiner(", ");
        globals.forEach(g -> globJ.add(g.getType() + " " + g.getName()));

        // 3) 结构体名列表字符串
        StringJoiner structJ = new StringJoiner(", ");
        structs.forEach(s -> structJ.add(s.name()));

        // 4) 函数名列表字符串
        StringJoiner funcJ = new StringJoiner(", ");
        functions.forEach(f -> funcJ.add(f.name()));

        // 5) 综合输出
        return "Module(name=" + name +
                ", imports=[" + impJ + "]" +
                ", globals=[" + globJ + "]" +
                ", structs=[" + structJ + "]" +
                ", functions=[" + funcJ + "])";
    }
}
