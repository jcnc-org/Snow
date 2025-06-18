package org.jcnc.snow.compiler.backend.generator;

import org.jcnc.snow.compiler.backend.util.IROpCodeMapper;
import org.jcnc.snow.compiler.backend.util.OpHelper;
import org.jcnc.snow.compiler.backend.builder.VMProgramBuilder;
import org.jcnc.snow.compiler.backend.core.InstructionGenerator;
import org.jcnc.snow.compiler.ir.instruction.IRCompareJumpInstruction;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.Map;

/**
 * 条件比较跳转指令生成器
 * <p>
 * 该类实现了 {@link InstructionGenerator} 接口，用于将 IR 中的条件比较跳转指令
 * 转换为虚拟机可执行的指令序列。主要流程是先将比较操作数加载到虚拟机栈中，生成比较操作码，
 * 并发出跳转到目标标签的指令。
 */
public class CmpJumpGenerator implements InstructionGenerator<IRCompareJumpInstruction> {

    /**
     * 返回该生成器所支持的指令类型。
     *
     * @return {@link IRCompareJumpInstruction} 的类对象
     */
    @Override
    public Class<IRCompareJumpInstruction> supportedClass() {
        return IRCompareJumpInstruction.class;
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

    /**
     * 生成条件比较跳转相关的虚拟机指令。
     *
     * @param ins        需要生成的条件比较跳转中间指令（IR）
     * @param out        虚拟机程序构建器，用于输出生成的指令
     * @param slotMap    虚拟寄存器到实际槽（slot）编号的映射表
     * @param currentFn  当前处理的函数名（可用于调试或作用域标识）
     */
    @Override
    public void generate(IRCompareJumpInstruction ins,
                         VMProgramBuilder out,
                         Map<IRVirtualRegister, Integer> slotMap,
                         String currentFn) {
        // 获取左操作数所在的寄存器槽编号
        int leftSlot = slotMap.get(ins.left());
        // 获取右操作数所在的寄存器槽编号
        int rightSlot = slotMap.get(ins.right());

        char lType = out.getSlotType(leftSlot); // 未登记默认 'I'
        char rType = out.getSlotType(rightSlot);

        char tType = promote(lType, rType); // 类型提升结果

        // 加载左操作数到虚拟机栈
        out.emit(OpHelper.opcode(str(lType) + "_LOAD") + " " + leftSlot);
        String cvt = convert(lType, tType);
        if (cvt != null) out.emit(OpHelper.opcode(cvt));

        // 加载右操作数到虚拟机栈
        out.emit(OpHelper.opcode(str(rType) + "_LOAD") + " " + rightSlot);
        cvt = convert(rType, tType);
        if (cvt != null) out.emit(OpHelper.opcode(cvt));

        // 获取与当前比较操作对应的虚拟机操作码
        String cmpOp = IROpCodeMapper.toVMOp(ins.op());
        // 生成分支跳转指令，如果比较成立则跳转到目标标签
        out.emitBranch(OpHelper.opcode(cmpOp), ins.label());
    }
}
