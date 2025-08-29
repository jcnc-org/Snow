package org.jcnc.snow.compiler.ir.builder;

import org.jcnc.snow.compiler.ir.core.IRFunction;
import org.jcnc.snow.compiler.ir.core.IRInstruction;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;


/**
 * {@code IRContext} 类负责封装当前正在构建的 IRFunction 实例
 * 以及与之配套的作用域管理（IRBuilderScope），
 * 并简化虚拟寄存器分配与 IR 指令添加操作。
 *
 * <p>本类提供以下核心功能:
 * <ul>
 *   <li>持有并操作当前 IRFunction 对象；</li>
 *   <li>管理变量名与虚拟寄存器的映射关系；</li>
 *   <li>分配新的虚拟寄存器实例；</li>
 *   <li>将生成的 IRInstruction 自动添加到 IRFunction 中；</li>
 *   <li>支持声明阶段临时类型记录（如变量声明时的类型推断/校验）；</li>
 * </ul>
 * <b>注：</b> 该类一般不直接暴露给最终用户，仅供 IR 构建器内部流程调用。
 */
public class IRContext {

    /** label 自动生成计数器，用于保证唯一性 */
    private int labelCounter = 0;

    /** 当前正在构建的 IRFunction 对象，所有 IR 指令均将添加至此 */
    private final IRFunction function;

    /** 用于管理当前函数作用域内变量与虚拟寄存器、类型等的映射关系 */
    private final IRBuilderScope scope;

    /** 当前 declare 编译阶段变量类型，不在变量声明流程时为 null */
    private String varType;

    /**
     * 构造一个新的 IRContext，并将指定的 IRFunction 与作用域管理器关联。
     *
     * @param function 要构建的 IRFunction 实例（不可为 null）
     */
    public IRContext(IRFunction function) {
        this.function = function;
        this.scope = new IRBuilderScope();
        // 作用域需知晓当前 IRFunction，以便为变量分配寄存器
        this.scope.attachFunction(function);
        this.varType = null;
    }

    /** 获取当前正在构建的 IRFunction 对象。 */
    public IRFunction getFunction() {
        return function;
    }

    /**
     * 获取当前函数的变量与寄存器映射作用域。
     * <p>包内可见，仅限 builder 包内部调用，避免被外部滥用。</p>
     */
    IRBuilderScope getScope() {
        return scope;
    }

    /** 为当前函数分配一个新的虚拟寄存器。 */
    public IRVirtualRegister newRegister() {
        return function.newRegister();
    }

    /**
     * 兼容用法：分配一个“临时虚拟寄存器”。
     * <p>
     * 目前直接委托给 {@link #newRegister()}，便于老代码兼容与简单用法。
     * 若将来有命名/临时寄存器区分的需要，可在此扩展实现。
     * </p>
     */
    public IRVirtualRegister newTempRegister() {
        return newRegister();
    }

    /** 将指定的 IRInstruction 添加到当前 IRFunction 的指令列表中。 */
    public void addInstruction(IRInstruction instr) {
        function.add(instr);
    }

    /**
     * 生成一个唯一标签名，如 L0、L1、L2...
     * <p>常用于条件跳转、分支合流等 IR 控制流构建场景。</p>
     *
     * @return 形如 "L0", "L1" 等的唯一字符串标签
     */
    public String newLabel() {
        return "L" + (labelCounter++);
    }

    /** 获取当前 declare 编译阶段变量类型（声明流程中临时记录） */
    public String getVarType() {
        return varType;
    }

    /** 设置当前 declare 编译阶段变量类型（一般在变量声明时赋值） */
    public void setVarType(String type) {
        this.varType = type;
    }

    /** 清除当前 declare 编译阶段变量类型（声明流程结束时调用） */
    public void clearVarType() {
        this.varType = null;
    }
}
