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
 * <b>CallGenerator - 将 IR {@code CallInstruction} 生成 VM 指令</b>
 *
 * <p>
 * 本类负责将中间表示（IR）中的函数调用指令 {@link CallInstruction} 转换为虚拟机（VM）指令。
 * 支持普通函数调用和特殊的 syscall 指令转换。
 * </p>
 *
 * <p>
 * <b>能力说明：</b>
 * <ul>
 *   <li>支持识别 {@code IRConstant} 直接字面量或已绑定到虚拟寄存器的字符串常量，直接降级为 {@code SYSCALL &lt;SUBCMD&gt;} 指令。</li>
 *   <li>对普通函数，完成参数加载、调用、返回值保存等指令生成。</li>
 * </ul>
 * </p>
 */
public class CallGenerator implements InstructionGenerator<CallInstruction> {

    /**
     * <虚拟寄存器 id, 对应的字符串常量>
     * <p>用于记录虚拟寄存器与其绑定字符串常量的映射。由 {@link LoadConstGenerator} 在编译期间填充。</p>
     */
    private static final Map<Integer, String> STRING_CONST_POOL = new HashMap<>();

    /**
     * 注册字符串常量到虚拟寄存器
     * <p>供 {@link LoadConstGenerator} 在加载字符串常量时调用。</p>
     *
     * @param regId 虚拟寄存器 id
     * @param value 字符串常量值
     */
    public static void registerStringConst(int regId, String value) {
        STRING_CONST_POOL.put(regId, value);
    }

    /**
     * 返回本生成器支持的 IR 指令类型（CallInstruction）
     */
    @Override
    public Class<CallInstruction> supportedClass() {
        return CallInstruction.class;
    }

    /**
     * 生成 VM 指令的主逻辑
     *
     * @param ins       当前 IR 指令（函数调用）
     * @param out       指令输出构建器
     * @param slotMap   IR 虚拟寄存器与物理槽位映射
     * @param currentFn 当前函数名
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
            // 子命令支持 IRConstant（直接字面量）或虚拟寄存器（需已绑定字符串）
            String subcmd;
            IRValue first = args.getFirst();

            if (first instanceof IRConstant(Object value)) {    // 直接字面量
                if (!(value instanceof String s))
                    throw new IllegalStateException("syscall 第一个参数必须是字符串常量");
                subcmd = s.toUpperCase(Locale.ROOT);

            } else if (first instanceof IRVirtualRegister vr) {   // 虚拟寄存器
                // 从常量池中查找是否已绑定字符串
                subcmd = Optional.ofNullable(STRING_CONST_POOL.get(vr.id()))
                        .orElseThrow(() ->
                                new IllegalStateException("未找到 syscall 字符串常量绑定: " + vr));
                subcmd = subcmd.toUpperCase(Locale.ROOT);

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

        /* ========== 普通函数调用 ========== */

        // ---------- 1. 推断返回值类型（非 void 返回时用） ----------
        char retType = 'I';  // 默认为整型
        if (!ins.getArguments().isEmpty()) {
            int firstSlot = slotMap.get((IRVirtualRegister) ins.getArguments().getFirst());
            retType = out.getSlotType(firstSlot);
            if (retType == '\0') retType = 'I';
        }

        // ---------- 2. 加载全部实参 ----------
        for (var arg : ins.getArguments()) {
            int slotId = slotMap.get((IRVirtualRegister) arg);
            char t = out.getSlotType(slotId);
            if (t == '\0') t = 'I';  // 默认整型
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
