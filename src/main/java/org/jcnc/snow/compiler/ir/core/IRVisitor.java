package org.jcnc.snow.compiler.ir.core;

import org.jcnc.snow.compiler.ir.instruction.*;

/**
 * {@code IRVisitor} 是中间表示（IR）指令体系的访问者接口。
 * <p>
 * 它定义了访问者模式的核心机制，通过对每种 {@link IRInstruction} 子类
 * 提供独立的 {@code visit} 方法，实现对指令的分发与处理。
 * 不同的访问者实现可用于执行不同任务，例如：
 * </p>
 * <ul>
 *   <li>{@code IRPrinter}：打印指令内容</li>
 *   <li>{@code IROptimizer}：分析与重写 IR 以优化性能</li>
 *   <li>{@code IRCodeGenerator}：生成平台相关的机器码或汇编代码</li>
 * </ul>
 *
 * <p>
 * 每当添加新的 {@code IRInstruction} 子类，应同步扩展该接口，
 * 以确保访问行为的一致性与完整性。
 * </p>
 */
public interface IRVisitor {

    /**
     * 访问加法指令（示例实现）。
     *
     * @param inst 加法指令实例
     */
    void visit(IRAddInstruction inst);

    /**
     * 访问跳转指令。
     *
     * @param inst 跳转指令实例
     */
    void visit(IRJumpInstruction inst);

    /**
     * 访问返回指令（无返回值）。
     *
     * @param inst 返回指令实例
     */
    void visit(IRReturnInstruction inst);

    /**
     * 访问二元运算指令（如加减乘除等）。
     *
     * @param inst 二元运算 IR 指令实例
     */
    void visit(BinaryOperationInstruction inst);

    /**
     * 访问加载常量的指令。
     *
     * @param inst 常量加载指令实例
     */
    void visit(LoadConstInstruction inst);

    /**
     * 访问返回指令（支持返回值）。
     *
     * @param inst 通用返回指令实例
     */
    void visit(ReturnInstruction inst);

    /**
     * 访问一元运算指令（如取负等）。
     *
     * @param inst 一元运算指令实例
     */
    void visit(UnaryOperationInstruction inst);

    /**
     * 访问函数调用指令。
     *
     * @param instruction 函数调用指令实例
     */
    void visit(CallInstruction instruction);
}
