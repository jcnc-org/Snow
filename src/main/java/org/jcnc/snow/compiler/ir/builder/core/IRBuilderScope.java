package org.jcnc.snow.compiler.ir.builder.core;

import org.jcnc.snow.compiler.ir.core.IRFunction;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * {@code IRBuilderScope} 用于管理单个函数作用域内的变量名与虚拟寄存器的映射关系，以及相关类型、常量信息和结构体元数据。
 * <p>
 * 主要职责包括：
 * <ul>
 *   <li>维护当前作用域内所有已声明变量的虚拟寄存器和类型信息</li>
 *   <li>支持变量与虚拟寄存器的重新绑定与查找</li>
 *   <li>支持变量类型和编译期常量值的记录与查询</li>
 *   <li>支持跨模块全局常量（如 ModuleA.a）查找</li>
 *   <li>维护结构体字段布局（全局共享）：字段名 → 槽位下标，用于对象字段读写</li>
 *   <li>维护结构体继承关系（子类 → 父类），super(...) 调用会用到</li>
 * </ul>
 */
public final class IRBuilderScope {

    /**
     * 结构体字段布局表（全局共享）：结构体名 → (字段名 → 槽位下标)
     */
    private static final Map<String, Map<String, Integer>> STRUCT_LAYOUTS = new HashMap<>();
    /**
     * 结构体继承关系表（全局共享）：子类名 → 父类名
     */
    private static final Map<String, String> STRUCT_PARENTS = new HashMap<>();
    /**
     * 变量名到虚拟寄存器的映射表（本地变量，仅当前作用域）
     */
    private final Map<String, IRVirtualRegister> vars = new HashMap<>();
    /**
     * 变量名到类型字符串的映射表
     */
    private final Map<String, String> varTypes = new HashMap<>();
    /**
     * 变量名到编译期常量值的映射表（本作用域）
     */
    private final Map<String, Object> varConstValues = new HashMap<>();
    /**
     * 存放跨模块导入的全局常量（如 ModuleA.a）
     */
    private final Map<String, Object> externalConsts = new HashMap<>();
    /**
     * 当前作用域所绑定的 IRFunction 实例，用于变量分配新寄存器等。
     */
    private IRFunction fn;

    // ---------------- 结构体全局布局/继承 注册与查询 ----------------

    /**
     * 注册结构体字段布局。注册后该结构体名对应的字段布局会被全局保存。
     *
     * @param structName   结构体名
     * @param fieldToIndex 字段名到槽位下标的映射表
     */
    static void registerStructLayout(String structName, Map<String, Integer> fieldToIndex) {
        if (structName == null || fieldToIndex == null) return;
        STRUCT_LAYOUTS.put(structName, new HashMap<>(fieldToIndex));
    }

    /**
     * 获取结构体的字段布局。
     * 支持全限定名与简单名查找。
     *
     * @param structName 结构体名或全限定名
     * @return 字段名到槽位下标的不可变映射表；若未注册则返回 null
     */
    public static Map<String, Integer> getStructLayout(String structName) {
        Map<String, Integer> layout = STRUCT_LAYOUTS.get(structName);
        if (layout == null && structName != null) {
            int dot = structName.lastIndexOf('.');
            if (dot >= 0 && dot + 1 < structName.length()) {
                layout = STRUCT_LAYOUTS.get(structName.substring(dot + 1));
            }
        }
        return layout == null ? null : Collections.unmodifiableMap(layout);
    }

    /**
     * 注册结构体继承关系。
     *
     * @param structName 子类名
     * @param parentName 父类名
     */
    static void registerStructParent(String structName, String parentName) {
        if (structName == null || parentName == null || parentName.isBlank()) return;
        STRUCT_PARENTS.put(structName, parentName);
    }

    /**
     * 获取结构体父类名。支持全限定名和简单名的模糊查找。
     *
     * @param structName 结构体名
     * @return 父类名；如果未注册或无父类则返回 null
     */
    public static String getStructParent(String structName) {
        if (structName == null || structName.isBlank()) return null;
        String p = STRUCT_PARENTS.get(structName);
        if (p != null) return p;

        int dot = structName.lastIndexOf('.');
        if (dot >= 0 && dot + 1 < structName.length()) {
            String simple = structName.substring(dot + 1);
            p = STRUCT_PARENTS.get(simple);
            if (p != null) return p;
        }

        for (Map.Entry<String, String> e : STRUCT_PARENTS.entrySet()) {
            String k = e.getKey();
            int d = k.lastIndexOf('.');
            if (d >= 0 && d + 1 < k.length() && k.substring(d + 1).equals(structName)) {
                return e.getValue();
            }
        }
        return null;
    }

    // ---------------- 作用域绑定 & 变量声明 ----------------

    /**
     * 绑定当前作用域至指定 IRFunction 对象。
     * 后续变量声明、寄存器分配依赖于该函数上下文。
     *
     * @param fn 待绑定的 IRFunction 实例
     */
    void attachFunction(IRFunction fn) {
        this.fn = fn;
    }

    /**
     * 声明一个新变量，分配虚拟寄存器并记录类型。
     * 同名变量会被覆盖。
     *
     * @param name 变量名
     * @param type 变量类型字符串
     */
    public void declare(String name, String type) {
        IRVirtualRegister reg = fn.newRegister();
        vars.put(name, reg);
        varTypes.put(name, type);
        varConstValues.remove(name);
    }

    /**
     * 声明一个新变量，指定虚拟寄存器并记录类型。
     * 用于特殊情况（如参数寄存器直接传入）。
     *
     * @param name 变量名
     * @param type 变量类型字符串
     * @param reg  指定的虚拟寄存器
     */
    public void declare(String name, String type, IRVirtualRegister reg) {
        vars.put(name, reg);
        varTypes.put(name, type);
        varConstValues.remove(name);
    }

    /**
     * 更新指定变量所绑定的虚拟寄存器。
     * 不改变类型信息。
     *
     * @param name 变量名
     * @param reg  新虚拟寄存器
     */
    void put(String name, IRVirtualRegister reg) {
        vars.put(name, reg);
    }

    /**
     * 查找变量对应的虚拟寄存器。
     *
     * @param name 变量名
     * @return 虚拟寄存器；不存在则返回 null
     */
    public IRVirtualRegister lookup(String name) {
        return vars.get(name);
    }

    /**
     * 查找变量的类型字符串。
     *
     * @param name 变量名
     * @return 类型字符串；不存在则返回 null
     */
    public String lookupType(String name) {
        return varTypes.get(name);
    }

    /**
     * 获取当前作用域下所有变量的类型信息。
     *
     * @return 不可变映射表：变量名 → 类型字符串
     */
    public Map<String, String> getVarTypes() {
        return Collections.unmodifiableMap(varTypes);
    }

    // ---------------- 编译期常量与导入 ----------------

    /**
     * 设置变量的编译期常量值。value=null 时将移除常量绑定。
     *
     * @param name  变量名
     * @param value 常量值对象；null 表示移除
     */
    public void setConstValue(String name, Object value) {
        if (value == null) varConstValues.remove(name);
        else varConstValues.put(name, value);
    }

    /**
     * 获取变量的编译期常量值。
     * 若当前作用域未绑定，则查找导入的全局常量。
     *
     * @param name 变量名（或导入常量名）
     * @return 常量值对象；不存在则返回 null
     */
    public Object getConstValue(String name) {
        Object v = varConstValues.get(name);
        if (v != null) return v;
        return externalConsts.get(name);
    }

    /**
     * 清除变量的编译期常量值绑定。
     *
     * @param name 变量名
     */
    public void clearConstValue(String name) {
        varConstValues.remove(name);
    }

    /**
     * 导入外部模块的全局常量。
     *
     * @param qualifiedName 全限定名（如 ModuleA.a）
     * @param value         常量值对象
     */
    public void importExternalConst(String qualifiedName, Object value) {
        externalConsts.put(qualifiedName, value);
    }
}
