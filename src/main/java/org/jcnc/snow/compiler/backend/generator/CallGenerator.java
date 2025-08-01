package org.jcnc.snow.compiler.backend.generator;

import org.jcnc.snow.compiler.backend.builder.VMProgramBuilder;
import org.jcnc.snow.compiler.backend.core.InstructionGenerator;
import org.jcnc.snow.compiler.backend.utils.OpHelper;
import org.jcnc.snow.compiler.ir.common.GlobalFunctionTable;
import org.jcnc.snow.compiler.ir.core.IRValue;
import org.jcnc.snow.compiler.ir.instruction.CallInstruction;
import org.jcnc.snow.compiler.ir.value.IRConstant;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.vm.engine.VMOpCode;

import java.util.*;

/**
 * {@code CallGenerator} 负责将 IR 层的 {@link CallInstruction} 生成对应的 VM 层函数调用指令。
 * <p>
 * 支持：
 * <ul>
 *     <li>普通函数调用的参数压栈、调用、返回值保存</li>
 *     <li>特殊的 {@code syscall} 指令转为 VM 的 SYSCALL 指令</li>
 *     <li>数组访问内置函数 {@code __index_i(arr, idx)} 的专用指令序列</li>
 * </ul>
 * <p>
 * 对于 syscall 子命令，支持常量字符串和字符串寄存器两种来源，并支持寄存器-字符串常量池注册机制。
 * </p>
 */
public class CallGenerator implements InstructionGenerator<CallInstruction> {

    /**
     * 字符串常量池：用于绑定虚拟寄存器 id 到字符串值（供 syscall 子命令使用）。
     */
    private static final Map<Integer, String> STRING_CONST_POOL = new HashMap<>();

    /**
     * 注册一个字符串常量，绑定到虚拟寄存器 id。
     *
     * @param regId  虚拟寄存器 id
     * @param value  字符串常量
     */
    public static void registerStringConst(int regId, String value) {
        STRING_CONST_POOL.put(regId, value);
    }

    /**
     * 返回当前指令生成器支持的 IR 指令类型（即 {@link CallInstruction}）。
     *
     * @return {@code CallInstruction.class}
     */
    @Override
    public Class<CallInstruction> supportedClass() {
        return CallInstruction.class;
    }

    /**
     * 生成 VM 指令序列，实现函数调用/特殊 syscall/数组索引等 IR 指令的转换。
     *
     * @param ins        当前函数调用 IR 指令
     * @param out        VM 指令输出构建器
     * @param slotMap    IR 虚拟寄存器 → VM 槽位映射表
     * @param currentFn  当前函数名
     */
    @Override
    public void generate(CallInstruction ins,
                         VMProgramBuilder out,
                         Map<IRVirtualRegister, Integer> slotMap,
                         String currentFn) {

        /* ========== 特殊处理 syscall 调用 ========== */
        if ("syscall".equals(ins.getFunctionName()) ||
                ins.getFunctionName().endsWith(".syscall")) {

            List<IRValue> args = ins.getArguments();
            if (args.isEmpty()) {
                throw new IllegalStateException("syscall 需要子命令参数");
            }

            // ---------- 0. 解析 syscall 子命令 ----------
            // 支持 IRConstant 字面量或虚拟寄存器（需已绑定字符串）
            String subcmd;
            IRValue first = args.getFirst();

            if (first instanceof IRConstant(Object value)) {    // 直接字面量
                if (!(value instanceof String s))
                    throw new IllegalStateException("syscall 第一个参数必须是字符串常量");
                subcmd = s.toUpperCase(Locale.ROOT);

            } else if (first instanceof IRVirtualRegister vr) { // 来自寄存器的字符串
                String s = STRING_CONST_POOL.get(vr.id());
                if (s == null)
                    throw new IllegalStateException("未找到 syscall 字符串常量绑定: " + vr);
                subcmd = s.toUpperCase(Locale.ROOT);
            } else {
                throw new IllegalStateException("syscall 第一个参数必须是字符串常量");
            }

            // ---------- 1. 压栈其余 syscall 参数（index 1 开始） ----------
            for (int i = 1; i < args.size(); i++) {
                IRVirtualRegister vr = (IRVirtualRegister) args.get(i);
                int slotId = slotMap.get(vr);
                char t = out.getSlotType(slotId);
                if (t == '\0') t = 'I';  // 默认整型
                out.emit(OpHelper.opcode(t + "_LOAD") + " " + slotId);
            }

            // ---------- 2. 生成 SYSCALL 指令 ----------
            out.emit(VMOpCode.SYSCALL + " " + subcmd);
            return;   // syscall 无返回值，直接返回
        }

        /* ========== 特殊处理内部索引函数：__index_i(arr, idx) ========== */
        if ("__index_i".equals(ins.getFunctionName())) {
            // 加载参数（arr 为引用类型，idx 为整型）
            if (ins.getArguments().size() != 2) {
                throw new IllegalStateException("__index_i 需要两个参数(arr, idx)");
            }
            IRVirtualRegister arr = (IRVirtualRegister) ins.getArguments().get(0);
            IRVirtualRegister idx = (IRVirtualRegister) ins.getArguments().get(1);

            int arrSlot = slotMap.get(arr);
            int idxSlot = slotMap.get(idx);

            char arrT = out.getSlotType(arrSlot);
            if (arrT == '\0') arrT = 'R'; // 默认为引用类型
            out.emit(OpHelper.opcode(arrT + "_LOAD") + " " + arrSlot);

            char idxT = out.getSlotType(idxSlot);
            if (idxT == '\0') idxT = 'I'; // 默认为整型
            out.emit(OpHelper.opcode(idxT + "_LOAD") + " " + idxSlot);

            // 调用 SYSCALL ARR_GET，让 VM 取出数组元素并压回栈顶
            out.emit(VMOpCode.SYSCALL + " " + "ARR_GET");

            // 取回返回值并保存（当前仅支持 int 元素）
            int destSlot = slotMap.get(ins.getDest());
            out.emit(OpHelper.opcode("I_STORE") + " " + destSlot);
            out.setSlotType(destSlot, 'I');
            return;
        }

        /* ========== 普通函数调用 ========== */

        // ---------- 1. 推断返回值类型（非 void 返回时用） ----------
        char retType = 'I';  // 默认为整型
        if (!ins.getArguments().isEmpty()) {
            // 简化：根据第一个参数类型推断返回类型，或者通过全局表拿到返回类型
            String ret = GlobalFunctionTable.getReturnType(ins.getFunctionName());
            if (ret != null) {
                retType = Character.toUpperCase(ret.charAt(0));
            }
        }

        // ---------- 2. 压栈所有参数 ----------
        for (IRValue arg : ins.getArguments()) {
            IRVirtualRegister vr = (IRVirtualRegister) arg;
            int slotId = slotMap.get(vr);
            char t = out.getSlotType(slotId);
            if (t == '\0') t = 'I';
            out.emit(OpHelper.opcode(t + "_LOAD") + " " + slotId);
        }

        // ---------- 3. 发出 CALL 指令 ----------
        out.emitCall(ins.getFunctionName(), ins.getArguments().size());

        // ---------- 3.5 如果为 void 返回直接结束 ----------
        if ("void".equals(GlobalFunctionTable.getReturnType(ins.getFunctionName()))) {
            return;
        }

        // ---------- 4. 保存返回值 ----------
        int destSlot = slotMap.get(ins.getDest());
        out.emit(OpHelper.opcode(retType + "_STORE") + " " + destSlot);
        out.setSlotType(destSlot, retType);
    }
}
