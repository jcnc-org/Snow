package org.jcnc.snow.compiler.backend.generator;

import org.jcnc.snow.compiler.backend.builder.VMProgramBuilder;
import org.jcnc.snow.compiler.backend.core.InstructionGenerator;
import org.jcnc.snow.compiler.backend.utils.OpHelper;
import org.jcnc.snow.compiler.ir.common.GlobalFunctionTable;
import org.jcnc.snow.compiler.ir.instruction.CallInstruction;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.Map;

/**
 * 将 IR CallInstruction 生成 VM 指令
 */
public class CallGenerator implements InstructionGenerator<CallInstruction> {

    @Override
    public Class<CallInstruction> supportedClass() {
        return CallInstruction.class;
    }

    @Override
    public void generate(CallInstruction ins,
                         VMProgramBuilder out,
                         Map<IRVirtualRegister, Integer> slotMap,
                         String currentFn) {

        /* 1. 推断返回值类型（用于非 void 情况下的 I/F/D/L_STORE） */
        char retType = 'I';
        if (!ins.getArguments().isEmpty()) {
            int firstSlot = slotMap.get((IRVirtualRegister) ins.getArguments().getFirst());
            retType = out.getSlotType(firstSlot);
            if (retType == '\0') retType = 'I';
        }

        /* 2. 依次加载实参 */
        for (var arg : ins.getArguments()) {
            int slotId = slotMap.get((IRVirtualRegister) arg);
            char t = out.getSlotType(slotId);
            if (t == '\0') t = 'I';
            out.emit(OpHelper.opcode(t + "_LOAD") + " " + slotId);
        }

        /* 3. 发出 CALL 指令 */
        out.emitCall(ins.getFunctionName(), ins.getArguments().size());

        /* 3.5 若被调用函数返回 void，则无需保存返回值 */
        String rt = GlobalFunctionTable.getReturnType(ins.getFunctionName());
        if ("void".equals(rt)) {
            return;   // 直接结束，无 _STORE
        }

        /* 4. 保存返回值到目标槽 */
        int destSlot = slotMap.get(ins.getDest());
        out.emit(OpHelper.opcode(retType + "_STORE") + " " + destSlot);
        out.setSlotType(destSlot, retType);
    }
}
