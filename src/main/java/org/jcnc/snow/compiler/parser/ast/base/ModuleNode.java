package org.jcnc.snow.compiler.parser.ast.base;

import org.jcnc.snow.compiler.parser.ast.DeclarationNode;
import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.parser.ast.ImportNode;
import org.jcnc.snow.compiler.parser.ast.StructNode;

import java.util.List;
import java.util.StringJoiner;

/**
 * {@code ModuleNode} 表示一个源文件或逻辑模块的 AST 根节点。
 *
 * <p>
 * 用于描述一个完整模块的全部语法内容，包括模块名、导入依赖、全局变量、结构体定义、函数定义等。
 * 作为语法树的顶层节点，供后续语义分析与 IR 生成统一处理。
 * </p>
 *
 * <p><b>字段说明：</b></p>
 * <ul>
 *   <li>{@code name} —— 模块名称，通常对应文件名或逻辑包名，要求唯一。</li>
 *   <li>{@code imports} —— import 导入列表，描述本模块依赖的其它模块名。</li>
 *   <li>{@code globals} —— 全局变量与常量声明列表，作用域为整个模块。</li>
 *   <li>{@code structs} —— 结构体定义列表（支持零个或多个结构体）。</li>
 *   <li>{@code functions} —— 函数定义列表（支持零个或多个函数）。</li>
 *   <li>{@code context} —— 源代码位置信息，用于报错定位与调试。</li>
 * </ul>
 *
 * <p>
 * 该类为 Java record，天然不可变。构造参数即为字段名，调用时自动生成 getter 方法。
 * </p>
 *
 * @param name      模块名称（如 "main"）
 * @param imports   导入依赖列表（ImportNode）
 * @param globals   全局变量声明列表（DeclarationNode）
 * @param structs   结构体定义列表（StructNode）
 * @param functions 函数定义列表（FunctionNode）
 * @param context   源代码位置信息（NodeContext）
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
     * 重写 toString 方法，便于调试和日志输出。
     * 展示模块名、导入、全局变量、结构体、函数的简要列表。
     *
     * @return 字符串形式，格式如：
     * Module(name=main, imports=[foo, bar], globals=[int x, ...], structs=[A, B], functions=[f, g])
     */
    @Override
    public String toString() {
        // 构造 import 字符串
        StringJoiner impJ = new StringJoiner(", ");
        imports.forEach(i -> impJ.add(i.moduleName()));

        // 构造全局变量声明字符串（类型+名称）
        StringJoiner globJ = new StringJoiner(", ");
        globals.forEach(g -> globJ.add(g.getType() + " " + g.getName()));

        // 构造结构体名列表
        StringJoiner structJ = new StringJoiner(", ");
        structs.forEach(s -> structJ.add(s.name()));

        // 构造函数名列表
        StringJoiner funcJ = new StringJoiner(", ");
        functions.forEach(f -> funcJ.add(f.name()));

        // 综合输出
        return "Module(name=" + name +
                ", imports=[" + impJ + "]" +
                ", globals=[" + globJ + "]" +
                ", structs=[" + structJ + "]" +
                ", functions=[" + funcJ + "])";
    }
}
