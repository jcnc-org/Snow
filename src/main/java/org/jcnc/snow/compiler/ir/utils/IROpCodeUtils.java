package org.jcnc.snow.compiler.ir.utils;

import org.jcnc.snow.compiler.ir.core.IROpCode;

import java.util.Map;

/**
 * IR 操作码辅助工具。
 */
public class IROpCodeUtils {
    private static final Map<IROpCode, IROpCode> INVERT = Map.ofEntries(
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
            Map.entry(IROpCode.CMP_LLE, IROpCode.CMP_LGT)
    );

    /**
     * 获取给定比较操作的“相反操作码”。
     */
    public static IROpCode invert(IROpCode op) {
        return INVERT.get(op);
    }
}
