package org.jcnc.snow.compiler.ir.builder;

import org.jcnc.snow.compiler.ir.core.IRFunction;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * {@code IRBuilderScope} 用于管理单个函数作用域内的变量名与虚拟寄存器的映射关系。
 * <p>
 * 主要职责：
 * <ul>
 *   <li>维护当前作用域内所有已声明变量的寄存器和类型信息</li>
 *   <li>支持变量与虚拟寄存器的重新绑定与查找</li>
 *   <li>支持变量的类型信息记录与查询</li>
 *   <li>支持变量的编译期常量值记录与查询（便于常量折叠等优化）</li>
 *   <li>支持跨模块全局常量（如 ModuleA.a）查找</li>
 *   <li>维护结构体字段布局（全局共享）：字段名 → 槽位下标，用于对象字段读写</li>
 * </ul>
 */
final class IRBuilderScope {

    /** 变量名到虚拟寄存器的映射表（本地变量） */
    private final Map<String, IRVirtualRegister> vars = new HashMap<>();
    /** 变量名到类型字符串的映射表 */
    private final Map<String, String> varTypes = new HashMap<>();
    /** 变量名到编译期常量值的映射表（本作用域） */
    private final Map<String, Object> varConstValues = new HashMap<>();

    /**
     * 存放跨模块导入的全局常量（如 ModuleA.a）。
     * 键为 "ModuleA.a"，值为常量值。
     */
    private final Map<String, Object> externalConsts = new HashMap<>();

    /**
     * 结构体字段布局的全局静态表：
     * 结构体名 → (字段名 → 槽位下标)。
     * 静态表设计，确保所有作用域、所有 IR 构建都能访问同一套布局。
     */
    private static final Map<String, Map<String, Integer>> STRUCT_LAYOUTS = new HashMap<>();

    /** 当前作用域所绑定的 IRFunction 实例，用于变量分配新寄存器等。 */
    private IRFunction fn;

    // ---------------- 作用域与变量 ----------------

    /**
     * 绑定当前作用域到指定 IRFunction。
     *
     * @param fn 目标 IRFunction
     */
    void attachFunction(IRFunction fn) {
        this.fn = fn;
    }

    /**
     * 声明一个新变量并分配新的虚拟寄存器。
     *
     * @param name 变量名称
     * @param type 变量类型名
     */
    void declare(String name, String type) {
        IRVirtualRegister reg = fn.newRegister();
        vars.put(name, reg);
        varTypes.put(name, type);
        varConstValues.remove(name); // 声明新变量即清除原常量绑定
    }

    /**
     * 声明新变量，并绑定到指定的寄存器。
     *
     * @param name 变量名称
     * @param type 变量类型名
     * @param reg  绑定的虚拟寄存器
     */
    void declare(String name, String type, IRVirtualRegister reg) {
        vars.put(name, reg);
        varTypes.put(name, type);
        varConstValues.remove(name); // 重复声明也会清除常量绑定
    }

    /**
     * 更新变量的虚拟寄存器绑定（如变量已存在则覆盖，否则等同于新声明）。
     *
     * @param name 变量名称
     * @param reg  新的虚拟寄存器
     */
    void put(String name, IRVirtualRegister reg) {
        vars.put(name, reg);
    }

    /**
     * 查找变量名对应的虚拟寄存器。
     *
     * @param name 变量名
     * @return 已绑定的虚拟寄存器，若未声明则返回 null
     */
    IRVirtualRegister lookup(String name) {
        return vars.get(name);
    }

    /**
     * 查找变量名对应的类型名。
     *
     * @param name 变量名
     * @return 已声明类型字符串，若未声明则返回 null
     */
    String lookupType(String name) {
        return varTypes.get(name);
    }

    /**
     * 获取变量名到类型名映射的不可变副本。
     *
     * @return 变量名→类型名映射的只读视图（用于调试/全局分析）
     */
    Map<String, String> getVarTypes() {
        return Collections.unmodifiableMap(varTypes);
    }

    // ---------------- 编译期常量相关接口 ----------------

    /**
     * 设置变量的编译期常量值（本地变量）。
     * <p>
     * 便于 IR 生成时做常量折叠等优化，value 传 null 则清除绑定。
     *
     * @param name  变量名称
     * @param value 常量值（null 表示清除）
     */
    void setConstValue(String name, Object value) {
        if (value == null) varConstValues.remove(name);
        else varConstValues.put(name, value);
    }

    /**
     * 获取变量的编译期常量值（优先本地，再查跨模块导入）。
     * <p>
     * 常用于优化与折叠，支持 "Module.a" 这类跨模块全局常量查找。
     *
     * @param name 变量名称或"模块名.常量名"
     * @return 编译期常量值，或 null
     */
    Object getConstValue(String name) {
        Object v = varConstValues.get(name);
        if (v != null) return v;
        // 支持跨模块常量/全局变量（如 "ModuleA.a"）
        return externalConsts.get(name);
    }

    /**
     * 清除变量的编译期常量值绑定（本地）。
     *
     * @param name 变量名称
     */
    void clearConstValue(String name) {
        varConstValues.remove(name);
    }

    // ---------------- 跨模块常量导入支持 ----------------

    /**
     * 导入外部（其他模块）的全局常量/变量。
     *
     * @param qualifiedName 形如 "ModuleA.a"
     * @param value         其常量值
     */
    void importExternalConst(String qualifiedName, Object value) {
        externalConsts.put(qualifiedName, value);
    }

    // ---------------- 结构体字段布局（全局静态） ----------------

    /**
     * 全局注册结构体的字段布局映射（字段名 -> 槽位下标）。
     * 一般在语义分析/IR 前期由类型系统收集后调用。
     *
     * @param structName   结构体名（建议为简单名，如 "Animal"；如有模块前缀也可）
     * @param fieldToIndex 字段名到下标的映射（下标从 0 递增）
     */
    static void registerStructLayout(String structName, Map<String, Integer> fieldToIndex) {
        if (structName == null || fieldToIndex == null) return;
        // 覆盖式注册：方便增量/重复编译时刷新
        STRUCT_LAYOUTS.put(structName, new HashMap<>(fieldToIndex));
    }

    /**
     * 查询字段槽位下标。
     * 支持“模块.结构体”及简单名两种写法自动兼容。
     *
     * @param structName 结构体名（"Module.Struct" 或 "Struct"）
     * @param fieldName  字段名
     * @return 槽位下标；若未知返回 null
     */
    Integer lookupFieldIndex(String structName, String fieldName) {
        // 先按原样查
        Map<String, Integer> layout = STRUCT_LAYOUTS.get(structName);
        // 兼容 “模块.结构体” 的写法：若没命中，退化为简单名再查
        if (layout == null && structName != null) {
            int dot = structName.lastIndexOf('.');
            if (dot >= 0 && dot + 1 < structName.length()) {
                String simple = structName.substring(dot + 1);
                layout = STRUCT_LAYOUTS.get(simple);
            }
        }
        if (layout == null) return null;
        return layout.get(fieldName);
    }

    /**
     * 读取某结构体的完整字段布局（返回只读 Map）。
     * 支持“模块.结构体”及简单名两种写法。
     *
     * @param structName 结构体名
     * @return 字段名到下标映射的只读视图，或 null
     */
    Map<String, Integer> getStructLayout(String structName) {
        Map<String, Integer> layout = STRUCT_LAYOUTS.get(structName);
        if (layout == null && structName != null) {
            int dot = structName.lastIndexOf('.');
            if (dot >= 0 && dot + 1 < structName.length()) {
                layout = STRUCT_LAYOUTS.get(structName.substring(dot + 1));
            }
        }
        return layout == null ? null : Collections.unmodifiableMap(layout);
    }
}
