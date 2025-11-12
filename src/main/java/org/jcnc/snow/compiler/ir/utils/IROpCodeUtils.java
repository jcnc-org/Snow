package org.jcnc.snow.compiler.ir.utils;

import org.jcnc.snow.compiler.ir.core.IROpCode;

import java.util.Map;

/**
 * IR 操作码辅助工具。
 */
public class IROpCodeUtils {
    private static final Map<IROpCode, IROpCode> INVERT = Map.ofEntries(
            // 8-bit
            Map.entry(IROpCode.CMP_BEQ, IROpCode.CMP_BNE),
            Map.entry(IROpCode.CMP_BNE, IROpCode.CMP_BEQ),
            Map.entry(IROpCode.CMP_BLT, IROpCode.CMP_BGE),
            Map.entry(IROpCode.CMP_BGE, IROpCode.CMP_BLT),
            Map.entry(IROpCode.CMP_BGT, IROpCode.CMP_BLE),
            Map.entry(IROpCode.CMP_BLE, IROpCode.CMP_BGT),
            // 16-bit
            Map.entry(IROpCode.CMP_SEQ, IROpCode.CMP_SNE),
            Map.entry(IROpCode.CMP_SNE, IROpCode.CMP_SEQ),
            Map.entry(IROpCode.CMP_SLT, IROpCode.CMP_SGE),
            Map.entry(IROpCode.CMP_SGE, IROpCode.CMP_SLT),
            Map.entry(IROpCode.CMP_SGT, IROpCode.CMP_SLE),
            Map.entry(IROpCode.CMP_SLE, IROpCode.CMP_SGT),
            // 32-bit
            Map.entry(IROpCode.CMP_IEQ, IROpCode.CMP_INE),
            Map.entry(IROpCode.CMP_INE, IROpCode.CMP_IEQ),
            Map.entry(IROpCode.CMP_ILT, IROpCode.CMP_IGE),
            Map.entry(IROpCode.CMP_IGE, IROpCode.CMP_ILT),
            Map.entry(IROpCode.CMP_IGT, IROpCode.CMP_ILE),
            Map.entry(IROpCode.CMP_ILE, IROpCode.CMP_IGT),
            // 64-bit
            Map.entry(IROpCode.CMP_LEQ, IROpCode.CMP_LNE),
            Map.entry(IROpCode.CMP_LNE, IROpCode.CMP_LEQ),
            Map.entry(IROpCode.CMP_LLT, IROpCode.CMP_LGE),
            Map.entry(IROpCode.CMP_LGE, IROpCode.CMP_LLT),
            Map.entry(IROpCode.CMP_LGT, IROpCode.CMP_LLE),
            Map.entry(IROpCode.CMP_LLE, IROpCode.CMP_LGT),
            // float
            Map.entry(IROpCode.CMP_FEQ, IROpCode.CMP_FNE),
            Map.entry(IROpCode.CMP_FNE, IROpCode.CMP_FEQ),
            Map.entry(IROpCode.CMP_FLT, IROpCode.CMP_FGE),
            Map.entry(IROpCode.CMP_FGE, IROpCode.CMP_FLT),
            Map.entry(IROpCode.CMP_FGT, IROpCode.CMP_FLE),
            Map.entry(IROpCode.CMP_FLE, IROpCode.CMP_FGT),
            // double
            Map.entry(IROpCode.CMP_DEQ, IROpCode.CMP_DNE),
            Map.entry(IROpCode.CMP_DNE, IROpCode.CMP_DEQ),
            Map.entry(IROpCode.CMP_DLT, IROpCode.CMP_DGE),
            Map.entry(IROpCode.CMP_DGE, IROpCode.CMP_DLT),
            Map.entry(IROpCode.CMP_DGT, IROpCode.CMP_DLE),
            Map.entry(IROpCode.CMP_DLE, IROpCode.CMP_DGT),
            Map.entry(IROpCode.CMP_REQ, IROpCode.CMP_RNE),
            Map.entry(IROpCode.CMP_RNE, IROpCode.CMP_REQ)
    );

    /**
     * 获取给定比较操作的“相反操作码”。
     */
    public static IROpCode invert(IROpCode op) {
        return INVERT.get(op);
    }
}
