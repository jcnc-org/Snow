package org.jcnc.snow.compiler.ir.builder.core;

import org.jcnc.snow.compiler.ir.core.IROpCode;
import org.jcnc.snow.compiler.ir.instruction.*;
import org.jcnc.snow.compiler.ir.value.IRConstant;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

/**
 * IR 指令统一生成工厂类。
 * <p>
 * 该类负责封装常量加载、二元运算、赋值、控制流、返回等 IR 指令生成逻辑，
 * 以提高 IR 生成阶段的可维护性与复用性。
 * <p>
 * 每个方法都会根据输入参数生成对应的 IR 指令并注册到 {@link IRContext} 中，
 * 返回值通常是存储结果的 {@link IRVirtualRegister}。
 */
public class InstructionFactory {

    /**
     * 加载一个 int 类型常量到新分配的寄存器中。
     *
     * @param ctx   当前 IR 上下文
     * @param value int 类型常量值
     * @return 存储该常量的新虚拟寄存器
     */
    public static IRVirtualRegister loadConst(IRContext ctx, int value) {
        IRVirtualRegister r = ctx.newRegister();
        ctx.addInstruction(new LoadConstInstruction(r, new IRConstant(value)));
        ctx.getScope().setRegisterType(r, "int");
        return r;
    }

    /**
     * 加载一个 long 类型常量到新寄存器。
     *
     * @param ctx   当前 IR 上下文
     * @param value long 类型常量值
     * @return 存储该常量的新虚拟寄存器
     */
    public static IRVirtualRegister loadConst(IRContext ctx, long value) {
        IRVirtualRegister r = ctx.newRegister();
        ctx.addInstruction(new LoadConstInstruction(r, new IRConstant(value)));
        ctx.getScope().setRegisterType(r, "long");
        return r;
    }

    /**
     * 加载一个 float 类型常量到新寄存器。
     *
     * @param ctx   当前 IR 上下文
     * @param value float 类型常量值
     * @return 存储该常量的新虚拟寄存器
     */
    public static IRVirtualRegister loadConst(IRContext ctx, float value) {
        IRVirtualRegister r = ctx.newRegister();
        ctx.addInstruction(new LoadConstInstruction(r, new IRConstant(value)));
        ctx.getScope().setRegisterType(r, "float");
        return r;
    }

    /**
     * 加载一个 double 类型常量到新寄存器。
     *
     * @param ctx   当前 IR 上下文
     * @param value double 类型常量值
     * @return 存储该常量的新虚拟寄存器
     */
    public static IRVirtualRegister loadConst(IRContext ctx, double value) {
        IRVirtualRegister r = ctx.newRegister();
        ctx.addInstruction(new LoadConstInstruction(r, new IRConstant(value)));
        ctx.getScope().setRegisterType(r, "double");
        return r;
    }

    /**
     * 执行二元运算（如加法、减法等），
     * 将结果写入新分配的虚拟寄存器并返回。
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
        markResultType(ctx, dest, op);
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
        markConstType(ctx, dest, value != null ? value.value() : null);
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
        markResultType(ctx, dest, op);
    }

    /**
     * 生成“值拷贝”语义（src → dest）。
     * <p>
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
                    op = IROpCode.ADD_I32;
                    zeroConst = new IRConstant(0);
                }
            }
        } else {
            op = IROpCode.ADD_I32;
            zeroConst = new IRConstant(0);
        }

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
     * 生成带返回值的 return 指令。
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

    /**
     * 根据操作码推断运算结果类型并标注到寄存器上。
     *
     * @param ctx  当前 IR 上下文
     * @param dest 目标寄存器
     * @param op   操作码
     */
    private static void markResultType(IRContext ctx, IRVirtualRegister dest, IROpCode op) {
        if (ctx == null || dest == null || op == null) return;
        String name = op.name();
        int idx = name.lastIndexOf('_');
        if (idx < 0 || idx == name.length() - 1) return;
        String suffix = name.substring(idx + 1);
        String type = switch (suffix) {
            case "B8" -> "byte";
            case "S16" -> "short";
            case "I32" -> "int";
            case "L64" -> "long";
            case "F32" -> "float";
            case "D64" -> "double";
            case "R" -> "string";
            default -> null;
        };
        if (type != null) {
            ctx.getScope().setRegisterType(dest, type);
        }
    }

    /**
     * 根据常量值类型标注目标寄存器类型。
     *
     * @param ctx   当前 IR 上下文
     * @param dest  目标寄存器
     * @param value 常量值对象
     */
    private static void markConstType(IRContext ctx, IRVirtualRegister dest, Object value) {
        if (ctx == null || dest == null || value == null) return;
        String type = switch (value) {
            case Byte ignored -> "byte";
            case Short ignored -> "short";
            case Integer ignored -> "int";
            case Long ignored -> "long";
            case Float ignored -> "float";
            case Double ignored -> "double";
            case String ignored -> "string";
            default -> null;
        };
        if (type != null) {
            ctx.getScope().setRegisterType(dest, type);
        }
    }
}
