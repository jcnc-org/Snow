package org.jcnc.snow.compiler.ir.builder;

import org.jcnc.snow.compiler.ir.core.IROpCode;
import org.jcnc.snow.compiler.ir.instruction.*;
import org.jcnc.snow.compiler.ir.value.IRConstant;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

/**
 * InstructionFactory —— 统一生成并注册 IR 指令的工厂类。
 * <p>
 * 该类封装了常见的 IR 指令生成方式，包括常量加载、二元运算、赋值、控制流等，
 * 统一简化指令插入和寄存器分配逻辑，提升 IR 生成阶段的代码可维护性和复用性。
 * </p>
 */
public class InstructionFactory {

    /* ====================================================================== */
    /*                     常量 / 通用二元运算（新寄存器）                     */
    /* ====================================================================== */

    /**
     * 加载整数常量，将其写入一个新分配的虚拟寄存器，并返回该寄存器。
     *
     * @param ctx   当前 IR 上下文（用于分配寄存器与添加指令）
     * @param value 要加载的整数常量值
     * @return 存储该常量的新虚拟寄存器
     */
    public static IRVirtualRegister loadConst(IRContext ctx, int value) {
        IRVirtualRegister r = ctx.newRegister();
        ctx.addInstruction(new LoadConstInstruction(r, new IRConstant(value)));
        return r;
    }

    /**
     * 执行二元运算（如加法、减法等），结果写入新分配的虚拟寄存器并返回该寄存器。
     *
     * @param ctx 当前 IR 上下文
     * @param op  运算类型（IROpCode 枚举，如 ADD_I32 等）
     * @param a   第一个操作数寄存器
     * @param b   第二个操作数寄存器
     * @return 保存运算结果的新虚拟寄存器
     */
    public static IRVirtualRegister binOp(IRContext ctx, IROpCode op,
                                          IRVirtualRegister a, IRVirtualRegister b) {
        IRVirtualRegister dest = ctx.newRegister();
        ctx.addInstruction(new BinaryOperationInstruction(op, dest, a, b));
        return dest;
    }

    /* ====================================================================== */
    /*                        直接写入指定寄存器                    */
    /* ====================================================================== */

    /**
     * 加载整数常量到指定虚拟寄存器。
     *
     * @param ctx   当前 IR 上下文
     * @param dest  目标寄存器
     * @param value 要加载的整数常量
     */
    public static void loadConstInto(IRContext ctx, IRVirtualRegister dest, int value) {
        ctx.addInstruction(new LoadConstInstruction(dest, new IRConstant(value)));
    }

    /**
     * 对两个寄存器执行二元运算，将结果写入指定目标寄存器。
     *
     * @param ctx  当前 IR 上下文
     * @param op   运算类型（IROpCode 枚举）
     * @param a    第一个操作数寄存器
     * @param b    第二个操作数寄存器
     * @param dest 运算结果目标寄存器
     */
    public static void binOpInto(IRContext ctx, IROpCode op,
                                 IRVirtualRegister a, IRVirtualRegister b,
                                 IRVirtualRegister dest) {
        ctx.addInstruction(new BinaryOperationInstruction(op, dest, a, b));
    }

    /**
     * Move 指令（src → dest）。若寄存器相同也安全。
     * <p>
     * 实现方式：dest = src + 0（即加上常量 0）。
     * </p>
     *
     * @param ctx  当前 IR 上下文
     * @param src  源寄存器
     * @param dest 目标寄存器
     */
    public static void move(IRContext ctx, IRVirtualRegister src, IRVirtualRegister dest) {
        // 自赋值无需任何操作，避免生成多余的常量 0 寄存器
        if (src == dest) {
            return;
        }
        // 回退实现：dest = src + 0
        IRVirtualRegister zero = loadConst(ctx, 0);
        ctx.addInstruction(new BinaryOperationInstruction(IROpCode.ADD_I32, dest, src, zero));
    }

    /* ====================================================================== */
    /*                            控制流指令                                  */
    /* ====================================================================== */

    /**
     * 生成无条件跳转（JMP）指令，跳转到指定标签。
     *
     * @param ctx   当前 IR 上下文
     * @param label 目标标签名
     */
    public static void jmp(IRContext ctx, String label) {
        ctx.addInstruction(new IRJumpInstruction(label));
    }

    /**
     * 在 IR 中插入一个标签（Label）。
     *
     * @param ctx   当前 IR 上下文
     * @param label 标签名
     */
    public static void label(IRContext ctx, String label) {
        ctx.addInstruction(new IRLabelInstruction(label));
    }

    /**
     * 比较跳转（如 if a < b goto label），根据条件跳转到目标标签。
     *
     * @param ctx         当前 IR 上下文
     * @param cmp         比较操作码（如 IROpCode.LT_I32 等）
     * @param a           第一个操作数寄存器
     * @param b           第二个操作数寄存器
     * @param targetLabel 跳转目标标签
     */
    public static void cmpJump(IRContext ctx, IROpCode cmp,
                               IRVirtualRegister a, IRVirtualRegister b,
                               String targetLabel) {
        ctx.addInstruction(new IRCompareJumpInstruction(cmp, a, b, targetLabel));
    }

    /* ---------------- 返回 ---------------- */

    /**
     * 生成返回指令（带返回值）。
     *
     * @param ctx   当前 IR 上下文
     * @param value 返回值寄存器
     */
    public static void ret(IRContext ctx, IRVirtualRegister value) {
        ctx.addInstruction(new ReturnInstruction(value));
    }

    /**
     * 生成无返回值的 return 指令（如 void 函数）。
     *
     * @param ctx 当前 IR 上下文
     */
    public static void retVoid(IRContext ctx) {
        ctx.addInstruction(new ReturnInstruction(null));
    }
}
