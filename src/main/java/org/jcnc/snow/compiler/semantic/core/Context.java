package org.jcnc.snow.compiler.semantic.core;

import org.jcnc.snow.compiler.semantic.analyzers.AnalyzerRegistry;
import org.jcnc.snow.compiler.semantic.error.SemanticError;
import org.jcnc.snow.compiler.semantic.type.ArrayType;
import org.jcnc.snow.compiler.semantic.type.Type;

import java.util.List;
import java.util.Map;

/**
 * {@code Context} 表示语义分析阶段的全局上下文环境。
 * <p>
 * 该类贯穿整个语义分析流程，集中管理以下内容：
 * <ul>
 *   <li>模块信息管理（所有已加载模块，包括源模块和内置模块）；</li>
 *   <li>错误收集（集中存储语义分析期间产生的 {@link SemanticError}）；</li>
 *   <li>日志输出控制（可选，支持调试信息）；</li>
 *   <li>分析器调度（通过 {@link AnalyzerRegistry} 分发对应分析器）；</li>
 * </ul>
 * <p>
 * 提供便捷的 getter 方法和类型解析工具方法。
 * </p>
 */
public class Context {
    /**
     * 模块表：模块名 → {@link ModuleInfo}，用于模块查找与跨模块引用。
     */
    private final Map<String, ModuleInfo> modules;

    /**
     * 错误列表：语义分析过程中收集的所有 {@link SemanticError}。
     */
    private final List<SemanticError> errors;

    /**
     * 日志开关：若为 true，将启用 {@link #log(String)} 输出日志信息。
     */
    private final boolean verbose;

    /**
     * 语义分析器注册表：用于按节点类型动态调度分析器。
     */
    private final AnalyzerRegistry registry;

    /**
     * 构造语义分析上下文对象。
     *
     * @param modules  已注册的模块信息集合
     * @param errors   错误收集器，分析器将所有语义错误写入此列表
     * @param verbose  是否启用调试日志输出
     * @param registry 分析器注册表，提供类型到分析器的映射与调度能力
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

    // ------------------ 模块信息 ------------------

    /**
     * 获取所有模块信息映射表。
     *
     * @return 模块名到模块信息（{@link ModuleInfo}）的映射表
     */
    public Map<String, ModuleInfo> getModules() {
        return modules;
    }

    /**
     * 模块信息 getter（快捷方式）。
     *
     * @return 模块名到模块信息（{@link ModuleInfo}）的映射表
     */
    public Map<String, ModuleInfo> modules() {
        return modules;
    }

    // ------------------ 错误收集 ------------------

    /**
     * 获取语义分析过程中记录的所有错误。
     *
     * @return 错误列表
     */
    public List<SemanticError> getErrors() {
        return errors;
    }

    /**
     * 错误列表 getter（快捷方式）。
     *
     * @return 错误列表
     */
    public List<SemanticError> errors() {
        return errors;
    }

    // ------------------ 分析器注册表 ------------------

    /**
     * 获取分析器注册表，用于分发语句与表达式分析器。
     *
     * @return {@link AnalyzerRegistry} 实例
     */
    public AnalyzerRegistry getRegistry() {
        return registry;
    }

    /**
     * 注册表 getter（快捷方式）。
     *
     * @return {@link AnalyzerRegistry} 实例
     */
    public AnalyzerRegistry registry() {
        return registry;
    }

    // ------------------ 日志输出 ------------------

    /**
     * 打印日志信息，仅当 {@code verbose} 为 true 时生效。
     *
     * @param msg 日志内容
     */
    public void log(String msg) {
        if (verbose) {
            System.out.println("[SemanticAnalyzer] " + msg);
        }
    }

    // ------------------ 工具函数 ------------------

    /**
     * 将类型名称字符串解析为对应的类型实例（支持多维数组后缀）。
     * <p>
     * 例如，"int" → int 类型，"int[][]" → 二维整型数组类型。
     * </p>
     *
     * @param name 类型名称（支持 "[]" 数组后缀）
     * @return 对应的 {@link Type} 实例，若无法识别返回 null
     */
    public Type parseType(String name) {
        int dims = 0;
        while (name.endsWith("[]")) {
            name = name.substring(0, name.length() - 2);
            dims++;
        }
        Type base = BuiltinTypeRegistry.BUILTIN_TYPES.get(name);
        if (base == null) return null;
        Type t = base;
        for (int i = 0; i < dims; i++) {
            t = new ArrayType(t);
        }
        return t;
    }
}
