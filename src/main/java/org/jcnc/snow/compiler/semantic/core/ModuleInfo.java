package org.jcnc.snow.compiler.semantic.core;

import org.jcnc.snow.compiler.semantic.symbol.SymbolTable;
import org.jcnc.snow.compiler.semantic.type.FunctionType;
import org.jcnc.snow.compiler.semantic.type.StructType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@code ModuleInfo} 表示单个模块在语义分析阶段的所有元信息封装。
 *
 * <p>
 * 每个模块对应一个唯一的 ModuleInfo 实例，在语义分析期间用于管理和查询：
 * <ul>
 *   <li>模块名称（全局唯一标识）；</li>
 *   <li>本模块导入的其他模块名集合（跨模块依赖支持）；</li>
 *   <li>该模块中所有函数签名（用于查找/重名检测/类型推断）；</li>
 *   <li>模块级全局符号表（常量、全局变量，支持类型推断和常量折叠）；</li>
 *   <li>本模块定义的结构体类型（struct）。</li>
 * </ul>
 * </p>
 *
 * <p><b>主要用途：</b>
 * <ul>
 *   <li>跨模块语义分析、符号查找、类型推断、常量查找、函数/类型重名校验等。</li>
 *   <li>所有对该模块的引用、类型检查、全局变量访问等均依赖本类提供的索引信息。</li>
 * </ul>
 * </p>
 */
public class ModuleInfo {

    /**
     * 模块名称，作为全局唯一标识符。
     * 不可为空。通常为文件名或逻辑模块名。
     */
    private final String name;

    /**
     * 该模块显式导入的模块名集合。
     * <p>
     * 仅存储本模块 import 的模块名（如 import foo;），
     * 用于限制跨模块访问符号时的合法性校验。
     * </p>
     * <p>
     * 注意：集合为内部引用，允许外部直接添加或删除导入关系。
     * </p>
     */
    private final Set<String> imports = new HashSet<>();

    /**
     * 该模块中声明的所有函数签名映射。
     * <p>
     * 键为函数名，值为函数类型。
     * 支持跨模块函数引用、重名检测和类型推断。
     * </p>
     */
    private final Map<String, FunctionType> functions = new HashMap<>();

    /**
     * 该模块中声明的所有结构体类型（struct）。
     * <p>
     * 键为结构体名称，值为结构体类型。
     * 用于跨模块/跨作用域结构体查找与类型检查。
     * </p>
     */
    private final Map<String, StructType> structs = new HashMap<>();

    /**
     * 模块级全局符号表。
     * <p>
     * 记录本模块声明的所有常量和全局变量。
     * 支持类型推断、跨模块常量折叠、全局符号查找。
     * 由 FunctionChecker 阶段注入。
     * </p>
     */
    private SymbolTable globals;

    /**
     * 构造函数：根据模块名初始化模块元信息。
     *
     * @param name 模块名称，要求全局唯一，且不可为空。
     */
    public ModuleInfo(String name) {
        this.name = name;
    }

    /**
     * 获取当前模块的全局唯一名称。
     *
     * @return 模块名字符串
     */
    public String getName() {
        return name;
    }

    /**
     * 获取本模块导入的所有模块名称集合。
     *
     * <p>
     * 注意：返回为内部集合引用，调用方可直接对集合进行 add/remove 操作维护导入依赖关系。
     * </p>
     *
     * @return 可变集合，包含所有导入模块名（如 "foo", "bar"）
     */
    public Set<String> getImports() {
        return imports;
    }

    /**
     * 获取模块内所有函数签名表。
     *
     * <p>
     * 键为函数名，值为函数类型。
     * 返回对象为内部映射，可用于动态添加/修改/删除函数定义。
     * </p>
     *
     * @return 函数名 → 函数类型映射表
     */
    public Map<String, FunctionType> getFunctions() {
        return functions;
    }

    /**
     * 获取模块定义的所有结构体类型。
     *
     * <p>
     * 键为结构体名，值为结构体类型描述。
     * </p>
     *
     * @return 结构体名 → 结构体类型映射
     */
    public Map<String, StructType> getStructs() {
        return structs;
    }

    /**
     * 获取当前模块的全局符号表（包含常量与全局变量）。
     *
     * <p>
     * 该符号表通常由 FunctionChecker 阶段在全局扫描时构建并注入。
     * 提供后续类型检查、常量折叠、跨模块全局符号访问等能力。
     * </p>
     *
     * @return 本模块全局符号表
     */
    public SymbolTable getGlobals() {
        return globals;
    }

    /**
     * 设置模块的全局符号表（仅限 FunctionChecker 构建时注入）。
     *
     * <p>
     * 通常仅语义分析的全局阶段由框架内部调用，
     * 不建议外部用户主动修改此符号表。
     * </p>
     *
     * @param globals 新的全局符号表实例
     */
    public void setGlobals(SymbolTable globals) {
        this.globals = globals;
    }
}
