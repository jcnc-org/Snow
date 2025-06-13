package org.jcnc.snow.compiler.backend.generator;

import org.jcnc.snow.compiler.backend.builder.VMProgramBuilder;
import org.jcnc.snow.compiler.backend.core.InstructionGenerator;
import org.jcnc.snow.compiler.backend.util.IROpCodeMapper;
import org.jcnc.snow.compiler.backend.util.OpHelper;
import org.jcnc.snow.compiler.ir.core.IRValue;
import org.jcnc.snow.compiler.ir.instruction.BinaryOperationInstruction;
import org.jcnc.snow.compiler.ir.value.IRConstant;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 二元运算指令生成器。
 * <p>
 * 负责将中间表示的二元运算指令（算术、位运算及比较运算）生成对应的虚拟机指令序列，
 * 并自动进行类型提升。
 * 同时实现 "+0 → MOV" 的 Peephole 优化，避免多余的 PUSH/ADD 序列。
 * </p>
 * <p>类型提升优先级：D &gt; F &gt; L &gt; I &gt; S &gt; B</p>
 */
public class BinaryOpGenerator implements InstructionGenerator<BinaryOperationInstruction> {

    /* -------------------- 常量与工具 -------------------- */

    /**
     * 用于生成唯一标签的计数器
     */
    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    /**
     * 生成一个新的唯一标签。
     *
     * @param fn  当前函数名，用于标签前缀
     * @param tag 标签用途标识
     * @return 形如 fn$tag$序号 的唯一标签
     */
    private static String fresh(String fn, String tag) {
        return fn + "$" + tag + "$" + COUNTER.getAndIncrement();
    }

    /**
     * 类型优先级：D &gt; F &gt; L &gt; I &gt; S &gt; B
     */
    private static int rank(char p) {
        return switch (p) {
            case 'D' -> 6;
            case 'F' -> 5;
            case 'L' -> 4;
            case 'I' -> 3;
            case 'S' -> 2;
            case 'B' -> 1;
            default -> 0;
        };
    }

    /**
     * 返回优先级更高的类型字符
     */
    private static char promote(char a, char b) {
        return rank(a) >= rank(b) ? a : b;
    }

    /**
     * 单字符转字符串
     */
    private static String str(char p) {
        return String.valueOf(p);
    }

    /**
     * 判断常量值是否等于 0。
     * 仅支持 Java 原生数值类型。
     */
    private static boolean isZero(Object v) {
        if (v == null) return false;
        return switch (v) {
            case Integer i -> i == 0;
            case Long l -> l == 0L;
            case Short s -> s == (short) 0;
            case Byte b -> b == (byte) 0;
            case Float f -> f == 0.0f;
            case Double d -> d == 0.0;
            default -> false;
        };
    }

    /**
     * 获取从类型 {@code from} 到 {@code to} 的转换指令名。
     * 相同类型或无显式转换需求返回 {@code null}。
     */
    private static String convert(char from, char to) {
        if (from == to) return null;
        return switch ("" + from + to) {
            case "IL" -> "I2L";
            case "ID" -> "I2D";
            case "IF" -> "I2F";
            case "LI" -> "L2I";
            case "LD" -> "L2D";
            case "LF" -> "L2F";
            case "FI" -> "F2I";
            case "FL" -> "F2L";
            case "FD" -> "F2D";
            case "DI" -> "D2I";
            case "DL" -> "D2L";
            case "DF" -> "D2F";
            case "SI" -> "S2I";
            case "BI" -> "B2I";
            default -> null;
        };
    }

    /* -------------------- 接口实现 -------------------- */

    @Override
    public Class<BinaryOperationInstruction> supportedClass() {
        return BinaryOperationInstruction.class;
    }

    @Override
    public void generate(BinaryOperationInstruction ins,
                         VMProgramBuilder out,
                         Map<IRVirtualRegister, Integer> slotMap,
                         String currentFn) {

        /* ---------- 0. +0 → MOV Peephole 优化 ---------- */
        String irName = ins.op().name();
        if (irName.startsWith("ADD_")) {
            IRValue lhs = ins.operands().getFirst();
            IRValue rhs = ins.operands().get(1);

            boolean lhsZero = lhs instanceof IRConstant && isZero(((IRConstant) lhs).value());
            boolean rhsZero = rhs instanceof IRConstant && isZero(((IRConstant) rhs).value());

            // 仅当一侧为常量 0 时可替换为 MOV
            if (lhsZero ^ rhsZero) {
                IRVirtualRegister srcVr = null;
                if ((lhsZero ? rhs : lhs) instanceof IRVirtualRegister) {
                    srcVr = (IRVirtualRegister) (lhsZero ? rhs : lhs);
                }
                int srcSlot = slotMap.get(srcVr);
                int destSlot = slotMap.get(ins.dest());

                // 源与目标槽位不同才需要发 MOV
                if (srcSlot != destSlot) {
                    out.emit(OpHelper.opcode("MOV") + " " + srcSlot + " " + destSlot);
                }
                // 复制槽位类型信息
                out.setSlotType(destSlot, out.getSlotType(srcSlot));
                return; // 优化路径结束
            }
        }

        /* ---------- 1. 槽位与类型 ---------- */
        int lSlot = slotMap.get((IRVirtualRegister) ins.operands().get(0));
        int rSlot = slotMap.get((IRVirtualRegister) ins.operands().get(1));
        int dSlot = slotMap.get(ins.dest());

        char lType = out.getSlotType(lSlot); // 未登记默认 'I'
        char rType = out.getSlotType(rSlot);

        char tType = promote(lType, rType); // 类型提升结果
        String tPre = str(tType);

        /* ---------- 2. 加载并做类型转换 ---------- */
        out.emit(OpHelper.opcode(str(lType) + "_LOAD") + " " + lSlot);
        String cvt = convert(lType, tType);
        if (cvt != null) out.emit(OpHelper.opcode(cvt));

        out.emit(OpHelper.opcode(str(rType) + "_LOAD") + " " + rSlot);
        cvt = convert(rType, tType);
        if (cvt != null) out.emit(OpHelper.opcode(cvt));

        /* ---------- 3. 区分算术 / 比较 ---------- */
        boolean isCmp = irName.startsWith("CMP_");

        /* === 3-A. 普通算术 / 位运算 === */
        if (!isCmp) {
            String opCore = irName.split("_")[0];          // ADD / SUB / MUL …
            out.emit(OpHelper.opcode(tPre + "_" + opCore));
            out.emit(OpHelper.opcode(tPre + "_STORE") + " " + dSlot);
            out.setSlotType(dSlot, tType);
            return;
        }

        /* === 3-B. CMP_* —— 生成布尔结果 === */
        String branchOp = OpHelper.opcode(IROpCodeMapper.toVMOp(ins.op())); // IC_E / IC_NE …
        String lblTrue = fresh(currentFn, "true");
        String lblEnd = fresh(currentFn, "end");

        // ① 条件跳转；成立 → lblTrue
        out.emitBranch(branchOp, lblTrue);

        // ② 不成立：压 0
        out.emit(OpHelper.opcode("I_PUSH") + " 0");
        out.emitBranch(OpHelper.opcode("JUMP"), lblEnd);

        // ③ 成立分支：压 1
        out.emit(lblTrue + ":");
        out.emit(OpHelper.opcode("I_PUSH") + " 1");

        // ④ 结束标签
        out.emit(lblEnd + ":");

        // ⑤ 写入目标槽位
        out.emit(OpHelper.opcode("I_STORE") + " " + dSlot);
        out.setSlotType(dSlot, 'I'); // 布尔 ➜ int
    }
}