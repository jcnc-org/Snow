package org.jcnc.snow.compiler.ir.builder;

import org.jcnc.snow.compiler.ir.core.IROpCode;
import org.jcnc.snow.compiler.ir.instruction.*;
import org.jcnc.snow.compiler.ir.value.IRConstant;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

/**
 * IR 指令统一生成工厂类，负责封装常量加载、二元运算、赋值、控制流等指令生成逻辑。
 * 提高 IR 生成阶段的可维护性与复用性。
 */
public class InstructionFactory {

    /**
     * 加载整数常量，将其写入一个新分配的虚拟寄存器，并返回该寄存器。
     *
     * @param ctx   当前 IR 上下文
     * @param value 整数常量值
     * @return 存储该常量的新虚拟寄存器
     */
    public static IRVirtualRegister loadConst(IRContext ctx, int value) {
        IRVirtualRegister r = ctx.newRegister();
        ctx.addInstruction(new LoadConstInstruction(r, new IRConstant(value)));
        return r;
    }

    /**
     * 加载 long 类型常量到新寄存器。
     *
     * @param ctx   当前 IR 上下文
     * @param value long 类型常量值
     * @return 存储该常量的新虚拟寄存器
     */
    public static IRVirtualRegister loadConst(IRContext ctx, long value) {
        IRVirtualRegister r = ctx.newRegister();
        ctx.addInstruction(new LoadConstInstruction(r, new IRConstant(value)));
        return r;
    }

    /**
     * 加载 float 类型常量到新寄存器。
     *
     * @param ctx   当前 IR 上下文
     * @param value float 类型常量值
     * @return 存储该常量的新虚拟寄存器
     */
    public static IRVirtualRegister loadConst(IRContext ctx, float value) {
        IRVirtualRegister r = ctx.newRegister();
        ctx.addInstruction(new LoadConstInstruction(r, new IRConstant(value)));
        return r;
    }

    /**
     * 加载 double 类型常量到新寄存器。
     *
     * @param ctx   当前 IR 上下文
     * @param value double 类型常量值
     * @return 存储该常量的新虚拟寄存器
     */
    public static IRVirtualRegister loadConst(IRContext ctx, double value) {
        IRVirtualRegister r = ctx.newRegister();
        ctx.addInstruction(new LoadConstInstruction(r, new IRConstant(value)));
        return r;
    }

    /**
     * 执行二元运算（如加法、减法等），结果写入新分配的虚拟寄存器并返回该寄存器。
     *
     * @param ctx 当前 IR 上下文
     * @param op  二元运算操作码
     * @param a   左操作数寄存器
     * @param b   右操作数寄存器
     * @return 存储结果的新虚拟寄存器
     */
    public static IRVirtualRegister binOp(IRContext ctx, IROpCode op, IRVirtualRegister a, IRVirtualRegister b) {
        IRVirtualRegister dest = ctx.newRegister();
        ctx.addInstruction(new BinaryOperationInstruction(op, dest, a, b));
        return dest;
    }

    /**
     * 加载常量到指定寄存器。
     *
     * @param ctx   当前 IR 上下文
     * @param dest  目标虚拟寄存器
     * @param value IR 常量值
     */
    public static void loadConstInto(IRContext ctx, IRVirtualRegister dest, IRConstant value) {
        ctx.addInstruction(new LoadConstInstruction(dest, value));
    }

    /**
     * 执行二元运算，并将结果写入指定寄存器。
     *
     * @param ctx  当前 IR 上下文
     * @param op   二元运算操作码
     * @param a    左操作数寄存器
     * @param b    右操作数寄存器
     * @param dest 目标虚拟寄存器
     */
    public static void binOpInto(IRContext ctx, IROpCode op, IRVirtualRegister a, IRVirtualRegister b, IRVirtualRegister dest) {
        ctx.addInstruction(new BinaryOperationInstruction(op, dest, a, b));
    }

    /**
     * 生成“值拷贝”语义（src → dest）。
     * 通过在 IR 中构造 “src + 0” 的形式，触发 Peephole 优化折叠成 MOV。
     *
     * @param ctx  当前 IR 上下文
     * @param src  源寄存器
     * @param dest 目标寄存器
     */
    public static void move(IRContext ctx, IRVirtualRegister src, IRVirtualRegister dest) {
        if (src == dest) {
            return;
        }
        String varType = ctx.getVarType();
        IROpCode op;
        IRConstant zeroConst;

        if (varType != null) {
            switch (varType) {
                case "byte" -> {
                    op = IROpCode.ADD_B8;
                    zeroConst = new IRConstant((byte) 0);
                }
                case "short" -> {
                    op = IROpCode.ADD_S16;
                    zeroConst = new IRConstant((short) 0);
                }
                case "int" -> {
                    op = IROpCode.ADD_I32;
                    zeroConst = new IRConstant(0);
                }
                case "long" -> {
                    op = IROpCode.ADD_L64;
                    zeroConst = new IRConstant(0L);
                }
                case "float" -> {
                    op = IROpCode.ADD_F32;
                    zeroConst = new IRConstant(0.0f);
                }
                case "double" -> {
                    op = IROpCode.ADD_D64;
                    zeroConst = new IRConstant(0.0);
                }
                default -> {
                    // 引用类型 / 结构体等统一走 int 路径
                    op = IROpCode.ADD_I32;
                    zeroConst = new IRConstant(0);
                }
            }
        } else {
            // 无法推断类型时，退化为 int 方案
            op = IROpCode.ADD_I32;
            zeroConst = new IRConstant(0);
        }

        // 注意：这里传入的是立即数 zeroConst，而不是寄存器
        ctx.addInstruction(new BinaryOperationInstruction(op, dest, src, zeroConst));
    }

    /**
     * 生成无条件跳转指令。
     *
     * @param ctx   当前 IR 上下文
     * @param label 跳转目标标签
     */
    public static void jmp(IRContext ctx, String label) {
        ctx.addInstruction(new IRJumpInstruction(label));
    }

    /**
     * 在 IR 流中插入标签。
     *
     * @param ctx   当前 IR 上下文
     * @param label 标签名
     */
    public static void label(IRContext ctx, String label) {
        ctx.addInstruction(new IRLabelInstruction(label));
    }

    /**
     * 比较两个寄存器，并根据结果跳转到指定标签。
     *
     * @param ctx         当前 IR 上下文
     * @param cmp         比较操作码
     * @param a           左操作数寄存器
     * @param b           右操作数寄存器
     * @param targetLabel 跳转目标标签
     */
    public static void cmpJump(IRContext ctx, IROpCode cmp, IRVirtualRegister a, IRVirtualRegister b, String targetLabel) {
        ctx.addInstruction(new IRCompareJumpInstruction(cmp, a, b, targetLabel));
    }

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
     * 生成无返回值（void）返回指令。
     *
     * @param ctx 当前 IR 上下文
     */
    public static void retVoid(IRContext ctx) {
        ctx.addInstruction(new ReturnInstruction(null));
    }
}
