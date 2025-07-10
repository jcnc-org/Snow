package org.jcnc.snow.compiler.ir.builder;

import org.jcnc.snow.compiler.ir.core.IRFunction;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.HashMap;
import java.util.Map;

/**
 * IRBuilderScope 用于管理单个函数内变量名与虚拟寄存器的映射关系。
 *
 * <p>主要功能包括：
 * <ul>
 *   <li>维护在当前作用域中已声明变量的寄存器分配信息；</li>
 *   <li>支持将已有虚拟寄存器与变量名重新绑定；</li>
 *   <li>根据变量名查找对应的虚拟寄存器实例或类型。</li>
 * </ul>
 */
final class IRBuilderScope {

    /**
     * 存储变量名到对应 IRVirtualRegister 的映射。
     * 变量名为键，虚拟寄存器对象为值，用于查找和更新。
     */
    private final Map<String, IRVirtualRegister> vars = new HashMap<>();

    /**
     * 存储变量名到对应类型的映射。
     * <br>
     * 变量名为键，变量类型为值，用于变量类型提升。
     */
    private final Map<String, String> varTypes = new HashMap<>();

    /**
     * 当前作用域所绑定的 IRFunction 对象，用于申请新的虚拟寄存器。
     */
    private IRFunction fn;

    /**
     * 将指定的 IRFunction 关联到当前作用域，以便后续声明变量时能够
     * 调用该函数的 newRegister() 方法生成新的寄存器。
     *
     * @param fn 要绑定到本作用域的 IRFunction 实例
     */
    void attachFunction(IRFunction fn) {
        this.fn = fn;
    }

    /**
     * 在当前作用域中声明一个新变量，并为其分配一个新的虚拟寄存器。
     * 调用绑定的 IRFunction.newRegister() 生成寄存器后保存到映射表中。
     *
     * @param name 变量名称，作为映射键使用
     * @param type 变量类型
     */
    void declare(String name, String type) {
        IRVirtualRegister reg = fn.newRegister();
        vars.put(name, reg);
        varTypes.put(name, type);
    }

    /**
     * 在当前作用域中声明或导入一个已有的虚拟寄存器，并将其与指定变量名绑定。
     * 该方法可用于将外部或前一作用域的寄存器导入到本作用域。
     *
     * @param name 变量名称，作为映射键使用
     * @param type 变量类型
     * @param reg  要绑定到该名称的 IRVirtualRegister 实例
     */
    void declare(String name, String type, IRVirtualRegister reg) {
        vars.put(name, reg);
        varTypes.put(name, type);
    }

    /**
     * 更新已存在变量的虚拟寄存器绑定关系。若变量已声明，则替换其对应的寄存器；
     * 若尚未声明，则等同于声明新变量。
     *
     * @param name 变量名称，作为映射键使用
     * @param reg  新的 IRVirtualRegister 实例，用于替换旧绑定
     */
    void put(String name, IRVirtualRegister reg) {
        vars.put(name, reg);
    }

    /**
     * 根据变量名称在当前作用域中查找对应的虚拟寄存器。
     *
     * @param name 需要查询的变量名称
     * @return 如果该名称已绑定寄存器，则返回对应的 IRVirtualRegister；
     *         如果未声明，则返回 null
     */
    IRVirtualRegister lookup(String name) {
        return vars.get(name);
    }

    /**
     * 根据变量名称在当前作用域中查找对应的类型。
     *
     * @param name 需要查询的变量名称
     * @return 如果该名称已声明，则返回对应的类型
     *         如果未声明，则返回 null
     */
    String lookupType(String name) {
        return varTypes.get(name);
    }

    /**
     * 获取 变量->类型的映射 的不可变副本
     * @return 变量->类型的映射 的不可变副本
     */
    Map<String, String> getVarTypes() {
        return Map.copyOf(varTypes);
    }
}
