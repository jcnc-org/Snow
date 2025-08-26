package org.jcnc.snow.compiler.semantic.core;

import org.jcnc.snow.compiler.semantic.symbol.SymbolTable;
import org.jcnc.snow.compiler.semantic.type.FunctionType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@code ModuleInfo} 表示单个模块在语义分析阶段的元信息封装。
 * <p>
 * 用于在分析期间管理模块间依赖、函数签名查找、全局符号表等关键任务。
 * 每个模块对应一个唯一的 {@code ModuleInfo} 实例，贯穿整个语义分析流程。
 *
 * <p><b>包含信息：</b>
 * <ul>
 *   <li>模块名称（全局唯一标识）；</li>
 *   <li>该模块导入的其他模块名集合（跨模块引用支持）；</li>
 *   <li>该模块中定义的所有函数签名 {@code Map<String, FunctionType>}；</li>
 *   <li>模块级全局符号表 {@link SymbolTable}（常量 / 全局变量，支持跨模块类型推断）。</li>
 * </ul>
 *
 * <p><b>典型用途：</b>
 * <ul>
 *   <li>用于函数签名类型查找、重名检测、跨模块引用校验等；</li>
 *   <li>全局符号表为类型检查与后端 IR 常量折叠等模块级分析提供支撑。</li>
 * </ul>
 */
public class ModuleInfo {

    /**
     * 模块名称，作为全局唯一标识
     */
    private final String name;

    /**
     * 该模块显式导入的模块名集合（用于跨模块访问符号）
     */
    private final Set<String> imports = new HashSet<>();

    /**
     * 该模块中定义的函数名 → 函数类型映射
     */
    private final Map<String, FunctionType> functions = new HashMap<>();

    /**
     * 模块级全局符号表（常量 / 全局变量）
     */
    private SymbolTable globals;

    /**
     * 构造模块信息对象。
     *
     * @param name 模块名称，必须唯一且不可为空
     */
    public ModuleInfo(String name) {
        this.name = name;
    }

    /**
     * 获取模块名称。
     *
     * @return 当前模块的唯一名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取该模块导入的模块名称集合。
     * <p>
     * 返回集合为内部数据的直接引用，调用方可通过 {@code add}/{@code remove} 方法动态维护导入信息。
     *
     * @return 可变集合，包含所有导入模块名
     */
    public Set<String> getImports() {
        return imports;
    }

    /**
     * 获取模块中已声明的函数签名表。
     * <p>
     * 映射键为函数名，值为对应的 {@link FunctionType}。
     * 返回对象为内部引用，可用于添加、修改或删除函数定义。
     *
     * @return 模块内函数定义映射表
     */
    public Map<String, FunctionType> getFunctions() {
        return functions;
    }

    /**
     * 获取模块的全局符号表（包含常量与全局变量）。
     * <p>
     * 该符号表由语义分析的 FunctionChecker 阶段构建完成并注入。
     * 提供跨模块类型检查、常量折叠等能力。
     *
     * @return 当前模块的全局符号表
     */
    public SymbolTable getGlobals() {
        return globals;
    }

    /**
     * 设置模块的全局符号表。
     * <p>
     * 仅应由 FunctionChecker 在语义分析全局扫描阶段调用。
     *
     * @param globals 全局符号表实例
     */
    public void setGlobals(SymbolTable globals) {
        this.globals = globals;
    }
}
