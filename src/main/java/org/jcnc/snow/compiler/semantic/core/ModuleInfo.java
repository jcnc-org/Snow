package org.jcnc.snow.compiler.semantic.core;

import org.jcnc.snow.compiler.semantic.type.FunctionType;

import java.util.*;

/**
 * {@code ModuleInfo} 表示单个模块在语义分析阶段的元信息封装。
 * <p>
 * 用于在分析期间管理模块间依赖、函数签名查找等关键任务。
 * 每个模块对应一个唯一的 {@code ModuleInfo} 实例。
 * <p>
 * 包含信息包括: 
 * <ul>
 *   <li>模块名称（唯一标识）；</li>
 *   <li>该模块导入的其他模块名集合；</li>
 *   <li>该模块中定义的所有函数签名 {@code Map<String, FunctionType>}。</li>
 * </ul>
 */
public class ModuleInfo {

    /** 模块名称，作为全局唯一标识 */
    private final String name;

    /** 该模块显式导入的模块名集合（用于跨模块访问符号） */
    private final Set<String> imports = new HashSet<>();

    /** 该模块中定义的函数名 → 函数类型映射 */
    private final Map<String, FunctionType> functions = new HashMap<>();

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
     * 返回集合为内部数据的直接引用，调用方可通过 {@code add/remove} 方法动态维护导入信息。
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

}
