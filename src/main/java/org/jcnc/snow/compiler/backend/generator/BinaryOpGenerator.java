package org.jcnc.snow.compiler.backend.generator;

import org.jcnc.snow.compiler.backend.builder.VMProgramBuilder;
import org.jcnc.snow.compiler.backend.core.InstructionGenerator;
import org.jcnc.snow.compiler.backend.util.IROpCodeMapper;
import org.jcnc.snow.compiler.backend.util.OpHelper;
import org.jcnc.snow.compiler.ir.instruction.BinaryOperationInstruction;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 二元运算指令生成器。
 * <p>
 * 负责将中间表示的二元运算指令（算术运算及比较运算）生成对应的虚拟机指令序列。
 * 支持对操作数进行自动类型提升，以保证运算结果的正确性。
 * </p>
 * <p>类型提升优先级：D &gt; F &gt; L &gt; I &gt; S &gt; B</p>
 */
public class BinaryOpGenerator implements InstructionGenerator<BinaryOperationInstruction> {

    /**
     * 用于生成唯一标签的计数器。
     */
    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    /**
     * 生成一个新的唯一标签。
     *
     * @param fn  当前函数名，用于标签前缀
     * @param tag 标签用途标识
     * @return  格式为 fn$tag$序号 的唯一标签字符串
     */
    private static String fresh(String fn, String tag) {
        return fn + "$" + tag + "$" + COUNTER.getAndIncrement();
    }

    /**
     * 返回类型字符对应的优先级。
     *
     * @param p 类型字符（例如 'D','F','L','I','S','B'）
     * @return  对应的优先级整数，数值越大优先级越高
     */
    private static int rank(char p) {
        return switch (p) {
            case 'D' -> 6;
            case 'F' -> 5;
            case 'L' -> 4;
            case 'I' -> 3;
            case 'S' -> 2;
            case 'B' -> 1;
            default  -> 0;
        };
    }

    /**
     * 比较两个类型字符，返回优先级更高的那个。
     *
     * @param a 左操作数类型
     * @param b 右操作数类型
     * @return  优先级更高者的类型字符
     */
    private static char promote(char a, char b) {
        return rank(a) >= rank(b) ? a : b;
    }

    /**
     * 将类型字符转换为字符串形式。
     *
     * @param p 类型字符
     * @return  长度为1的字符串
     */
    private static String str(char p) {
        return String.valueOf(p);
    }


    /**
     * 获取从类型 from 到类型 to 的转换指令名称。
     *
     * @param from 源类型字符
     * @param to   目标类型字符
     * @return     对应的指令名称，如 "I2L"；若两类型相同或不可转换则返回 null
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
            default   -> null;
        };
    }

    /**
     * 返回该生成器支持的指令类型。
     *
     * @return BinaryOperationInstruction 的 Class 对象
     */
    @Override
    public Class<BinaryOperationInstruction> supportedClass() {
        return BinaryOperationInstruction.class;
    }

    /**
     * 根据中间表示的二元运算指令生成对应的虚拟机指令序列。
     *
     * <p>步骤：</p>
     * <ol>
     *   <li>获取操作数与目标操作数寄存槽位及其类型。</li>
     *   <li>将左右操作数加载到栈并根据需要进行类型转换。</li>
     *   <li>区分算术/位运算与比较运算，分别生成不同指令序列：</li>
     *   <ul>
     *     <li>算术/位运算：直接调用对应的运算指令并保存结果。</li>
     *     <li>比较运算：使用条件跳转生成布尔结果。</li>
     *   </ul>
     *   <li>将结果存回目标槽位，并更新槽位类型。</li>
     * </ol>
     *
     * @param ins       中间表示的二元运算指令实例
     * @param out       字节码生成器，用于输出虚拟机指令
     * @param slotMap   虚拟寄存器到槽位编号的映射
     * @param currentFn 当前函数名，用于生成唯一标签
     */
    @Override
    public void generate(BinaryOperationInstruction ins,
                         VMProgramBuilder out,
                         Map<IRVirtualRegister, Integer> slotMap,
                         String currentFn) {

        /* ---------- 1. 槽位与类型 ---------- */
        int lSlot = slotMap.get((IRVirtualRegister) ins.operands().get(0));
        int rSlot = slotMap.get((IRVirtualRegister) ins.operands().get(1));
        int dSlot = slotMap.get(ins.dest());

        char lType = out.getSlotType(lSlot);   // 如未登记默认 'I'
        char rType = out.getSlotType(rSlot);

        char tType  = promote(lType, rType);   // 类型提升结果
        String tPre = str(tType);

        /* ---------- 2. 加载并做类型转换 ---------- */
        out.emit(OpHelper.opcode(str(lType) + "_LOAD") + " " + lSlot);
        String cvt = convert(lType, tType);
        if (cvt != null) out.emit(OpHelper.opcode(cvt));

        out.emit(OpHelper.opcode(str(rType) + "_LOAD") + " " + rSlot);
        cvt = convert(rType, tType);
        if (cvt != null) out.emit(OpHelper.opcode(cvt));

        /* ---------- 3. 区分算术 / 比较 ---------- */
        String irName = ins.op().name();
        boolean isCmp = irName.startsWith("CMP_");

        /* === 3-A. 普通算术 / 位运算 === */
        if (!isCmp) {
            String opName = irName.split("_")[0];                 // ADD / SUB / MUL …
            out.emit(OpHelper.opcode(tPre + "_" + opName));       // I_ADD / D_MUL …
            out.emit(OpHelper.opcode(tPre + "_STORE") + " " + dSlot);
            out.setSlotType(dSlot, tType);
            return;
        }

        /* === 3-B. CMP_* —— 生成布尔结果 === */
        String branchOp = OpHelper.opcode(IROpCodeMapper.toVMOp(ins.op())); // IC_E / IC_NE …
        String lblTrue  = fresh(currentFn, "true");
        String lblEnd   = fresh(currentFn, "end");

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
        out.setSlotType(dSlot, 'I');        // 布尔 ➜ int
    }
}
