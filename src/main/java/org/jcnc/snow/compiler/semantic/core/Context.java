package org.jcnc.snow.compiler.semantic.core;

import org.jcnc.snow.compiler.semantic.analyzers.AnalyzerRegistry;
import org.jcnc.snow.compiler.semantic.error.SemanticError;
import org.jcnc.snow.compiler.semantic.type.ArrayType;
import org.jcnc.snow.compiler.semantic.type.Type;

import java.util.List;
import java.util.Map;

/**
 * {@code Context} 表示语义分析阶段的全局上下文环境，贯穿于编译器语义分析流程中。
 * <p>
 * 主要负责：
 * <ul>
 *   <li>模块信息的统一管理（含模块、结构体、类型）；</li>
 *   <li>收集和存储语义分析阶段产生的所有错误；</li>
 *   <li>根据配置输出调试日志；</li>
 *   <li>调度各类语义分析器执行；</li>
 *   <li>类型解析（包括内建类型、数组类型、结构体等自定义类型）。</li>
 * </ul>
 * 该类为所有分析器提供便捷的访问、查找和工具方法。
 */
public class Context {
    /**
     * 所有模块信息表。
     * <p>
     * 键为模块名，值为 {@link ModuleInfo} 实例。用于支持跨模块类型/结构体引用。
     */
    private final Map<String, ModuleInfo> modules;

    /**
     * 语义分析期间收集的所有错误对象。
     * <p>
     * 每当发现语义错误时，将 {@link SemanticError} 添加到该列表。
     */
    private final List<SemanticError> errors;

    /**
     * 日志输出开关。
     * <p>
     * 为 true 时，调用 {@link #log(String)} 会输出日志信息，用于调试。
     */
    private final boolean verbose;

    /**
     * 语义分析器注册表。
     * <p>
     * 用于根据语法树节点类型动态分派对应的分析器。
     */
    private final AnalyzerRegistry registry;

    /**
     * 当前正在处理的模块名。
     * <p>
     * 主要用于类型解析时限定结构体查找的作用域。
     */
    private String currentModuleName;

    /**
     * 构造一个新的语义分析上下文实例。
     *
     * @param modules  所有已加载模块的映射表
     * @param errors   用于收集错误的列表
     * @param verbose  是否输出日志
     * @param registry 分析器注册表
     */
    public Context(Map<String, ModuleInfo> modules,
                   List<SemanticError> errors,
                   boolean verbose,
                   AnalyzerRegistry registry) {
        this.modules = modules;
        this.errors = errors;
        this.verbose = verbose;
        this.registry = registry;
    }

    // ==== Getter 方法（基本访问器） ====

    /**
     * 获取所有模块信息表（模块名 → ModuleInfo）。
     */
    public Map<String, ModuleInfo> modules() {
        return modules;
    }

    /**
     * 获取所有模块信息表（与 {@link #modules()} 等价）。
     */
    public Map<String, ModuleInfo> getModules() {
        return modules;
    }

    /**
     * 获取语义分析期间收集的所有错误。
     */
    public List<SemanticError> errors() {
        return errors;
    }

    /**
     * 获取语义分析期间收集的所有错误（与 {@link #errors()} 等价）。
     */
    public List<SemanticError> getErrors() {
        return errors;
    }

    /**
     * 获取语义分析器注册表。
     */
    public AnalyzerRegistry getRegistry() {
        return registry;
    }

    /**
     * 输出语义分析日志信息（受 verbose 控制）。
     *
     * @param msg 日志消息内容
     */
    public void log(String msg) {
        if (verbose) System.out.println("[semantic] " + msg);
    }

    // ==== 当前模块管理 ====

    /**
     * 获取当前正在分析的模块名。
     *
     * @return 当前模块名，可能为 null
     */
    public String getCurrentModule() {
        return currentModuleName;
    }

    /**
     * 设置当前正在分析的模块名。
     * <p>
     * 用于限定结构体查找的优先作用域。
     *
     * @param moduleName 当前模块名
     */
    public void setCurrentModule(String moduleName) {
        this.currentModuleName = moduleName;
    }

    // ==== 类型解析工具 ====

    /**
     * 解析类型字符串为 {@link Type} 实例。
     * <p>
     * 支持内建类型、数组类型（带 "[]" 后缀）、用户自定义结构体类型。
     * 类型解析的查找顺序为：<br>
     * 1. 内建类型；<br>
     * 2. 当前模块定义的结构体类型；<br>
     * 3. 当前模块导入模块中的结构体类型；<br>
     * 4. 全局所有模块的结构体类型。
     *
     * @param typeName 类型名称字符串，如 "int"、"Foo"、"Bar[][]"
     * @return 解析出的 {@link Type} 实例，若找不到则返回 null
     */
    public Type parseType(String typeName) {
        if (typeName == null) return null; // 处理空输入

        String name = typeName; // 剥离数组后缀前的基本类型名
        int dims = 0;           // 记录数组维度
        // 支持多维数组：如 "int[][]" -> dims=2
        while (name.endsWith("[]")) {
            name = name.substring(0, name.length() - 2);
            dims++;
        }

        // 1) 优先查找内建类型
        Type base = BuiltinTypeRegistry.BUILTIN_TYPES.get(name);

        // 2) 如果不是内建类型，则尝试查找结构体类型
        if (base == null) {
            // 2.1 当前模块下的结构体
            if (currentModuleName != null && modules.containsKey(currentModuleName)) {
                ModuleInfo mi = modules.get(currentModuleName);
                if (mi.getStructs().containsKey(name)) {
                    base = mi.getStructs().get(name);
                } else {
                    // 2.2 当前模块导入的模块中的结构体
                    for (String imp : mi.getImports()) {
                        ModuleInfo im = modules.get(imp);
                        if (im != null && im.getStructs().containsKey(name)) {
                            base = im.getStructs().get(name);
                            break;
                        }
                    }
                }
            }
            // 2.3 全局所有模块的结构体，作为兜底
            if (base == null) {
                for (ModuleInfo im : modules.values()) {
                    if (im.getStructs().containsKey(name)) {
                        base = im.getStructs().get(name);
                        break;
                    }
                }
            }
        }

        if (base == null) return null; // 所有路径均未找到

        // 包装数组类型：根据数组维度递归封装
        Type t = base;
        for (int i = 0; i < dims; i++) t = new ArrayType(t);
        return t;
    }
}
