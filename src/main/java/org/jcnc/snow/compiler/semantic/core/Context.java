package org.jcnc.snow.compiler.semantic.core;

import org.jcnc.snow.compiler.semantic.analyzers.AnalyzerRegistry;
import org.jcnc.snow.compiler.semantic.error.SemanticError;
import org.jcnc.snow.compiler.semantic.type.Type;

import java.util.List;
import java.util.Map;

/**
 * {@code Context} 表示语义分析阶段的共享上下文环境。
 * <p>
 * 它贯穿整个语义分析流程，用于维护并提供以下核心服务: 
 * <ul>
 *   <li>模块信息管理: 包含所有已加载模块（源模块与内置模块）；</li>
 *   <li>错误收集: 集中存储语义分析期间产生的 {@link SemanticError}；</li>
 *   <li>日志控制: 支持按需输出详细调试日志；</li>
 *   <li>分析器调度: 通过 {@link AnalyzerRegistry} 管理语句/表达式的分析器分发。</li>
 * </ul>
 */
public class Context {
    /** 模块表: 模块名 → {@link ModuleInfo}，用于模块查找与跨模块引用 */
    private final Map<String, ModuleInfo> modules;

    /** 错误列表: 语义分析过程中收集的所有 {@link SemanticError} */
    private final List<SemanticError> errors;

    /** 日志开关: 若为 true，将启用 {@link #log(String)} 输出日志信息 */
    private final boolean verbose;

    /** 语义分析器注册表: 用于按节点类型动态调度分析器 */
    private final AnalyzerRegistry registry;

    /**
     * 构造语义分析上下文对象。
     *
     * @param modules   已注册的模块信息集合
     * @param errors    错误收集器，分析器将所有语义错误写入此列表
     * @param verbose   是否启用调试日志输出
     * @param registry  分析器注册表，提供类型到分析器的映射与调度能力
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
     * @return 模块名 → 模块信息 {@link ModuleInfo} 的映射
     */
    public Map<String, ModuleInfo> getModules() {
        return modules;
    }

    /** @return 模块信息（快捷方式） */
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

    /** @return 错误列表（快捷方式） */
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

    /** @return 注册表（快捷方式） */
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
     * 将类型名称字符串解析为对应的内置 {@link Type} 实例。
     * <p>
     * 若类型在 {@link BuiltinTypeRegistry#BUILTIN_TYPES} 中存在，则返回对应类型；
     * 否则返回 {@code null}，调用方可据此决定是否降级处理。
     *
     * @param name 类型名称（如 "int", "float", "void", "string" 等）
     * @return 匹配的 {@link Type}，若无匹配项则返回 {@code null}
     */
    public Type parseType(String name) {
        return BuiltinTypeRegistry.BUILTIN_TYPES.get(name);
    }
}
