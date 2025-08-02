package org.jcnc.snow.compiler.ir.builder;

import org.jcnc.snow.compiler.ir.core.IRFunction;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

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
 * </ul>
 */
final class IRBuilderScope {

    /**
     * 变量名到虚拟寄存器的映射表。
     * 用于跟踪所有声明和分配的变量。
     */
    private final Map<String, IRVirtualRegister> vars = new HashMap<>();

    /**
     * 变量名到类型字符串的映射表。
     * 用于类型分析与推断。
     */
    private final Map<String, String> varTypes = new HashMap<>();
    /**
     * 变量名到编译期常量值的映射表。
     * 用于常量折叠优化（如 int、string、数组等常量）。
     */
    private final Map<String, Object> varConstValues = new HashMap<>();
    /**
     * 当前作用域所绑定的 IRFunction 实例。
     * 用于申请新的虚拟寄存器。
     */
    private IRFunction fn;

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
        varConstValues.remove(name);
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
        varConstValues.remove(name);
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
     * @return 变量名→类型名映射的只读视图
     */
    Map<String, String> getVarTypes() {
        return Map.copyOf(varTypes);
    }

    // ---------------- 编译期常量相关接口 ----------------

    /**
     * 设置变量的编译期常量值。
     *
     * @param name  变量名称
     * @param value 常量值（null 表示清除）
     */
    void setConstValue(String name, Object value) {
        if (value == null) varConstValues.remove(name);
        else varConstValues.put(name, value);
    }

    /**
     * 获取变量的编译期常量值（如没有则返回 null）。
     *
     * @param name 变量名称
     * @return 编译期常量值，或 null
     */
    Object getConstValue(String name) {
        return varConstValues.get(name);
    }

    /**
     * 清除变量的编译期常量值绑定。
     *
     * @param name 变量名称
     */
    void clearConstValue(String name) {
        varConstValues.remove(name);
    }
}
