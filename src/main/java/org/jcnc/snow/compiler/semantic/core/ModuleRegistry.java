package org.jcnc.snow.compiler.semantic.core;

import org.jcnc.snow.compiler.parser.ast.ModuleNode;

/**
 * {@code ModuleRegistry} 负责将用户源码中声明的模块名称注册至全局语义上下文。
 * <p>
 * 它会遍历语法树中的所有模块节点 {@link ModuleNode}，并将其模块名填入
 * {@link Context#modules()} 映射中，为后续语义分析阶段中的模块查找、
 * 导入验证和跨模块调用提供支持。
 * <p>
 * 注册结果为 {@code Map<String, ModuleInfo>}，键为模块名，值为新建的 {@link ModuleInfo}。
 * 若某模块名已存在（如内置模块），则不会重复注册。
 *
 * @param ctx 当前语义分析上下文，用于访问模块表
 */
public record ModuleRegistry(Context ctx) {

    /**
     * 构造模块注册器。
     *
     * @param ctx 当前语义分析上下文
     */
    public ModuleRegistry {
    }

    /**
     * 遍历并注册所有用户定义模块。
     * <p>
     * 对于每个模块节点，将其名称注册到 {@code ctx.modules()} 中作为键，
     * 若模块已存在（例如内置模块），则不会覆盖。
     *
     * @param mods 所有模块节点的集合
     */
    public void registerUserModules(Iterable<ModuleNode> mods) {
        for (ModuleNode mod : mods) {
            ctx.modules().putIfAbsent(mod.name(), new ModuleInfo(mod.name()));
        }
    }
}
