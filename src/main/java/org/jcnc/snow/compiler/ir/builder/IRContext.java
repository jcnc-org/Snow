package org.jcnc.snow.compiler.ir.builder;

import org.jcnc.snow.compiler.ir.core.IRFunction;
import org.jcnc.snow.compiler.ir.core.IRInstruction;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.Optional;

/**
 * IRContext 类负责封装当前正在构建的 IRFunction 实例
 * 以及与之配套的作用域管理（IRBuilderScope），
 * 并简化虚拟寄存器分配与 IR 指令添加操作。
 *
 * <p>本类提供以下核心功能：
 * <ul>
 *   <li>持有并操作当前 IRFunction 对象；</li>
 *   <li>管理变量名与虚拟寄存器的映射关系；</li>
 *   <li>分配新的虚拟寄存器实例；</li>
 *   <li>将生成的 IRInstruction 自动添加到 IRFunction 中；</li>
 * </ul>
 */
public class IRContext {

    /* 生成唯一标签用 */
    private int labelCounter = 0;
    /**
     * 当前正在构建的 IRFunction 对象，所有指令将添加至此
     */
    private final IRFunction function;

    /**
     * 用于管理当前函数作用域内变量与虚拟寄存器的映射
     */
    private final IRBuilderScope scope;

    /**
     * 当前声明变量的类型，不在声明变量时为空
     */
    private String varType;

    /**
     * 构造一个新的 IRContext，并将指定的 IRFunction 与作用域关联。
     *
     * @param function 要构建的 IRFunction 实例
     */
    public IRContext(IRFunction function) {
        this.function = function;
        this.scope = new IRBuilderScope();
        // 关联作用域与 IRFunction，以便在声明变量时申请寄存器
        this.scope.attachFunction(function);
        this.varType = null;
    }

    /**
     * 获取当前正在构建的 IRFunction 对象。
     *
     * @return 当前 IRFunction 实例
     */
    public IRFunction getFunction() {
        return function;
    }

    /**
     * 获取当前函数的变量与寄存器映射作用域。
     *
     * <p>包内可见：仅限 builder 包内部使用。
     *
     * @return IRBuilderScope 实例
     */
    IRBuilderScope getScope() {
        return scope;
    }

    /**
     * 为当前函数分配一个新的虚拟寄存器。
     *
     * @return 分配到的 IRVirtualRegister 对象
     */
    public IRVirtualRegister newRegister() {
        return function.newRegister();
    }

    /**
     * 将指定的 IRInstruction 添加到当前 IRFunction 的指令列表中。
     *
     * @param instr 要添加的 IRInstruction 实例
     */
    public void addInstruction(IRInstruction instr) {
        function.add(instr);
    }

    /** 生成一个形如 L0 / L1 ... 的唯一标签名 */
    public String newLabel() {
        return "L" + (labelCounter++);
    }

    /**
     * 获取当前 declare 编译阶段变量类型
     *
     * @return 当前 declare 的变量类型
     */
    public String getVarType() {
        return varType;
    }

    /**
     * 设置当前 declare 编译阶段变量类型
     *
     */
    public void setVarType(String type) {
        this.varType = type;
    }

    /**
     * 清除当前 declare 编译阶段变量类型
     *
     */
    public void clearVarType() {
        this.varType = null;
    }
}
