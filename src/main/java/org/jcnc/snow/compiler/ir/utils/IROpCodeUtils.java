package org.jcnc.snow.compiler.ir.utils;

import org.jcnc.snow.compiler.ir.core.IROpCode;

import java.util.Map;

/**
 * IR 操作码辅助工具。
 */
public class IROpCodeUtils {
    private static final Map<IROpCode, IROpCode> INVERT = Map.ofEntries(
            // 32-bit
            Map.entry(IROpCode.CMP_EQ, IROpCode.CMP_NE),
            Map.entry(IROpCode.CMP_NE, IROpCode.CMP_EQ),
            Map.entry(IROpCode.CMP_LT, IROpCode.CMP_GE),
            Map.entry(IROpCode.CMP_GE, IROpCode.CMP_LT),
            Map.entry(IROpCode.CMP_GT, IROpCode.CMP_LE),
            Map.entry(IROpCode.CMP_LE, IROpCode.CMP_GT),
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
