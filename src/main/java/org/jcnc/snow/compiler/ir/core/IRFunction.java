package org.jcnc.snow.compiler.ir.core;

import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 表示单个函数的中间表示（IR）。
 * <p>
 * IRFunction 跟踪代码生成和优化所需的所有信息，
 * 包括函数标识符、IR 指令序列、
 * 声明参数列表以及生成唯一虚拟寄存器的机制。
 * </p>
 */
public class IRFunction {

    /**
     * 函数名，对应源级函数的标识。
     */
    private final String name;

    /**
     * IR 指令列表，组成函数体。
     */
    private final List<IRInstruction> body = new ArrayList<>();
    /**
     * 正式参数所对应的虚拟寄存器列表，按声明顺序排列。
     */
    private final List<IRVirtualRegister> parameters = new ArrayList<>();
    /**
     * 寄存器到类型名的映射，用于在后端阶段传递类型信息。
     */
    private final Map<IRVirtualRegister, String> registerTypes = new HashMap<>();
    /**
     * 用于生成新的虚拟寄存器编号的计数器。
     */
    private int regCounter = 0;

    /**
     * 构造一个具有指定名称的 IRFunction 实例。
     *
     * @param name 要关联的函数名称
     */
    public IRFunction(String name) {
        this.name = name;
    }

    /**
     * 分配一个新的虚拟寄存器。
     * 每次调用会生成一个带有唯一编号的 IRVirtualRegister。
     *
     * @return 新分配的虚拟寄存器
     */
    public IRVirtualRegister newRegister() {
        return new IRVirtualRegister(regCounter++);
    }

    /**
     * 将一个虚拟寄存器添加到函数的正式参数列表中。
     * <p>
     * 应按源函数签名中参数的声明顺序逐一调用此方法。
     * </p>
     *
     * @param vr 表示函数某个参数的虚拟寄存器
     */
    public void addParameter(IRVirtualRegister vr) {
        parameters.add(vr);
    }

    /**
     * 获取函数正式参数的只读列表。
     *
     * @return 按声明顺序排列的虚拟寄存器列表
     */
    public List<IRVirtualRegister> parameters() {
        return List.copyOf(parameters);
    }

    /**
     * 向函数体末尾追加一条 IR 指令。
     *
     * @param inst 要追加的 IRInstruction 实例
     */
    public void add(IRInstruction inst) {
        body.add(inst);
    }

    /**
     * 获取函数体中所有指令的只读列表。
     *
     * @return 表示函数体的 IRInstruction 列表
     */
    public List<IRInstruction> body() {
        return List.copyOf(body);
    }

    /**
     * 获取函数的源级名称。
     *
     * @return 函数名称
     */
    public String name() {
        return name;
    }

    /**
     * 记录指定寄存器的类型。
     *
     * @param reg  寄存器
     * @param type 类型名
     */
    public void setRegisterType(IRVirtualRegister reg, String type) {
        if (reg == null) return;
        if (type == null || type.isBlank() || "void".equalsIgnoreCase(type)) {
            registerTypes.remove(reg);
        } else {
            registerTypes.put(reg, type);
        }
    }

    /**
     * 查询寄存器绑定的类型。
     *
     * @param reg 寄存器
     * @return 类型名或 null
     */
    public String getRegisterType(IRVirtualRegister reg) {
        return reg == null ? null : registerTypes.get(reg);
    }

    /**
     * 获取所有寄存器类型映射（只读视图）。
     */
    public Map<IRVirtualRegister, String> registerTypes() {
        return Collections.unmodifiableMap(registerTypes);
    }

    /**
     * 获取已分配的虚拟寄存器总数。
     *
     * @return 虚拟寄存器计数
     */
    public int registerCount() {
        return regCounter;
    }

    /**
     * 以IR代码表示，示例:
     * <pre>
     * func 名称(%0, %1, ...) {
     *   指令0
     *   指令1
     *   ...
     * }
     * </pre>
     *
     * @return 函数的 IR 文本表示
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("func ")
                .append(name)
                .append('(');
        for (int i = 0; i < parameters.size(); i++) {
            sb.append(parameters.get(i));
            if (i < parameters.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(") {\n");
        for (IRInstruction inst : body) {
            sb.append("  ").append(inst).append('\n');
        }
        sb.append('}');
        return sb.toString();
    }
}
