package org.jcnc.snow.compiler.backend.generator;

import org.jcnc.snow.compiler.backend.builder.VMProgramBuilder;
import org.jcnc.snow.compiler.backend.core.InstructionGenerator;
import org.jcnc.snow.compiler.backend.utils.IROpCodeMapper;
import org.jcnc.snow.compiler.backend.utils.OpHelper;
import org.jcnc.snow.compiler.backend.utils.TypePromoteUtils;
import org.jcnc.snow.compiler.ir.core.IROpCode;
import org.jcnc.snow.compiler.ir.core.IRValue;
import org.jcnc.snow.compiler.ir.instruction.BinaryOperationInstruction;
import org.jcnc.snow.compiler.ir.value.IRConstant;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@code BinaryOpGenerator} 用于生成虚拟机的二元运算指令。
 *
 * <p>
 * 功能：将中间表示（IR）的二元运算指令（如算术、位运算、比较）翻译为虚拟机指令序列，
 * 自动进行类型提升，并实现 "+0 → MOV" 的 Peephole 优化，消除无效加零运算。
 * </p>
 *
 * <p>类型提升优先级：D > F > L > I > S > B</p>
 *
 * <p>优化：</p>
 * <ul>
 *   <li>加法一侧为 0 时，优化为 MOV 指令。</li>
 *   <li>字符串拼接或对象运算路径采用引用类型（R_LOAD/R_ADD），防止类型转换异常。</li>
 * </ul>
 */
public class BinaryOpGenerator implements InstructionGenerator<BinaryOperationInstruction> {

    /*  常量与工具  */

    /**
     * 用于生成唯一标签的计数器
     */
    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    /**
     * 生成唯一标签。
     *
     * @param fn  当前函数名（前缀）
     * @param tag 标签用途（中缀）
     * @return 形如 fn$tag$序号 的唯一标签
     */
    private static String fresh(String fn, String tag) {
        return fn + "$" + tag + "$" + COUNTER.getAndIncrement();
    }

    /**
     * 判断常量值是否等于 0。
     * 仅支持 Java 原生数值类型。
     *
     * @param v 常量值
     * @return 若等于 0 返回 true，否则 false
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
     * 根据操作码推断其“主类型”前缀（B/S/I/L/F/D/R），用于在缺失类型信息时兜底。
     */
    private static char opcodeTypeHint(IROpCode op) {
        if (op == null) return 0;
        String name = op.name();
        int idx = name.indexOf('_');
        if (idx < 0) return 0;
        String suffix = name.substring(idx + 1);
        return switch (suffix) {
            case "B8" -> 'B';
            case "S16" -> 'S';
            case "I32" -> 'I';
            case "L64" -> 'L';
            case "F32" -> 'F';
            case "D64" -> 'D';
            case "R" -> 'R';
            case "BEQ", "BNE", "BLT", "BGT", "BLE", "BGE" -> 'B';
            case "SEQ", "SNE", "SLT", "SGT", "SLE", "SGE" -> 'S';
            case "IEQ", "INE", "ILT", "IGT", "ILE", "IGE" -> 'I';
            case "LEQ", "LNE", "LLT", "LGT", "LLE", "LGE" -> 'L';
            case "FEQ", "FNE", "FLT", "FGT", "FLE", "FGE" -> 'F';
            case "DEQ", "DNE", "DLT", "DGT", "DLE", "DGE" -> 'D';
            case "REQ", "RNE" -> 'R';
            default -> 0;
        };
    }

    /*  接口实现  */

    @Override
    public Class<BinaryOperationInstruction> supportedClass() {
        return BinaryOperationInstruction.class;
    }

    /**
     * 生成虚拟机的二元运算指令序列。
     *
     * <p>
     * 根据操作类型（算术/位运算/比较）和参与运算的槽类型，自动实现类型提升与 Peephole 优化，
     * 并区分引用类型（如字符串拼接）与数值类型指令生成路径。
     * </p>
     *
     * @param ins       二元运算 IR 指令
     * @param out       虚拟机程序构建器
     * @param slotMap   IR 寄存器与虚拟机槽位的映射
     * @param currentFn 当前函数名（用于唯一标签生成）
     */
    @Override
    public void generate(BinaryOperationInstruction ins,
                         VMProgramBuilder out,
                         Map<IRVirtualRegister, Integer> slotMap,
                         String currentFn) {

        // 0. +0 → MOV Peephole 优化
        String irName = ins.op().name();
        char opTypeHint = opcodeTypeHint(ins.op());
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
                char srcType = out.getSlotType(srcSlot);
                if (opTypeHint != 0 && opTypeHint != 'R' && srcType == 'R') {
                    srcType = opTypeHint;
                    out.setSlotType(srcSlot, srcType);
                }
                // 同步目标槽位类型信息，防止类型遗漏
                out.setSlotType(destSlot, srcType);
                return; // 优化路径结束
            }
        }

        // 1. 槽位与类型
        int lSlot = slotMap.get((IRVirtualRegister) ins.operands().get(0));
        int rSlot = slotMap.get((IRVirtualRegister) ins.operands().get(1));
        int dSlot = slotMap.get(ins.dest());

        char lType = out.getSlotType(lSlot); // 未登记时默认 'I'
        char rType = out.getSlotType(rSlot);
        if (opTypeHint != 0 && opTypeHint != 'R') {
            if (lType == 'R') {
                lType = opTypeHint;
                out.setSlotType(lSlot, lType);
            }
            if (rType == 'R') {
                rType = opTypeHint;
                out.setSlotType(rSlot, rType);
            }
        }

        char tType = TypePromoteUtils.promote(lType, rType); // 类型提升结果
        String tPre = TypePromoteUtils.str(tType);

        //  2. 加载并做类型转换
        if (tType == 'R') {
            // ★ 引用拼接路径（如字符串）：强制使用 R_LOAD，避免 I_LOAD 读取到 String 时 ClassCastException
            out.emit(OpHelper.opcode("R_LOAD") + " " + lSlot);
            out.emit(OpHelper.opcode("R_LOAD") + " " + rSlot);
        } else {
            // 数值/位运算路径：类型一致性及转换
            out.emit(OpHelper.opcode(TypePromoteUtils.str(lType) + "_LOAD") + " " + lSlot);
            String cvt = TypePromoteUtils.convert(lType, tType);
            if (cvt != null) out.emit(OpHelper.opcode(cvt));

            out.emit(OpHelper.opcode(TypePromoteUtils.str(rType) + "_LOAD") + " " + rSlot);
            cvt = TypePromoteUtils.convert(rType, tType);
            if (cvt != null) out.emit(OpHelper.opcode(cvt));
        }

        // 3. 区分算术 / 比较
        boolean isCmp = irName.startsWith("CMP_");

        // 3-A. 普通算术 / 位运算
        if (!isCmp) {
            String opCore = irName.split("_")[0]; // ADD / SUB / MUL …
            out.emit(OpHelper.opcode(tPre + "_" + opCore));
            out.emit(OpHelper.opcode(tPre + "_STORE") + " " + dSlot);
            out.setSlotType(dSlot, tType);
            return;
        }

        // 3-B. CMP_* —— 生成布尔结果
        String branchOp = OpHelper.opcode(IROpCodeMapper.toVMOp(ins.op())); // I_CE / I_CNE …
        String lblTrue = fresh(currentFn, "true");
        String lblEnd = fresh(currentFn, "end");

        // 1. 条件跳转；成立 → lblTrue
        out.emitBranch(branchOp, lblTrue);

        // 2. 不成立: 压 0
        out.emit(OpHelper.opcode("I_PUSH") + " 0");
        out.emitBranch(OpHelper.opcode("JUMP"), lblEnd);

        // 3. 成立分支: 压 1
        out.emit(lblTrue + ":");
        out.emit(OpHelper.opcode("I_PUSH") + " 1");

        // 4. 结束标签
        out.emit(lblEnd + ":");

        // 5. 写入目标槽位
        out.emit(OpHelper.opcode("I_STORE") + " " + dSlot);
        out.setSlotType(dSlot, 'I'); // 布尔 ➜ int
    }
}
