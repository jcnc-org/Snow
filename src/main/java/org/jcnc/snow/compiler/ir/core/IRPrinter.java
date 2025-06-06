package org.jcnc.snow.compiler.ir.core;

import org.jcnc.snow.compiler.ir.instruction.IRAddInstruction;
import org.jcnc.snow.compiler.ir.instruction.IRJumpInstruction;
import org.jcnc.snow.compiler.ir.instruction.IRReturnInstruction;

/**
 * {@code IRPrinter} 是一个用于打印 IR 指令的访问者实现。
 * <p>
 * 本类实现 {@link IRVisitor} 接口，通过覆盖各类指令的访问方法，
 * 提供对不同类型 IR 指令的格式化输出，通常用于调试或测试。
 * 默认行为是在控制台（System.out）输出指令的基本信息。
 * </p>
 * <p>
 * 可通过继承该类进一步扩展对更多指令类型的支持，或重写输出格式以适配不同的前端/后端需求。
 * </p>
 */
public abstract class IRPrinter implements IRVisitor {

    /**
     * 访问 {@link IRAddInstruction} 加法指令。
     * <p>
     * 默认输出形式为 "Add: <inst>"，其中 <inst> 为指令对象的字符串表示。
     * </p>
     *
     * @param inst 加法 IR 指令实例
     */
    @Override
    public void visit(IRAddInstruction inst) {
        System.out.println("Add: " + inst);
    }

    /**
     * 访问 {@link IRJumpInstruction} 跳转指令。
     * <p>
     * 默认输出形式为 "Jump: <inst>"。
     * </p>
     *
     * @param inst 跳转 IR 指令实例
     */
    @Override
    public void visit(IRJumpInstruction inst) {
        System.out.println("Jump: " + inst);
    }

    /**
     * 访问 {@link IRReturnInstruction} 返回指令。
     * <p>
     * 默认输出形式为 "Return: <inst>"。
     * </p>
     *
     * @param inst 返回 IR 指令实例
     */
    @Override
    public void visit(IRReturnInstruction inst) {
        System.out.println("Return: " + inst);
    }

}