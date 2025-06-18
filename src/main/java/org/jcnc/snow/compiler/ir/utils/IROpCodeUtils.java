package org.jcnc.snow.compiler.ir.utils;

import org.jcnc.snow.compiler.ir.core.IROpCode;

import java.util.Map;

/**
 * IR 操作码辅助工具。
 */
public class IROpCodeUtils {
    private static final Map<IROpCode, IROpCode> INVERT = Map.of(
        IROpCode.CMP_LEQ, IROpCode.CMP_LNE,
        IROpCode.CMP_LNE, IROpCode.CMP_LEQ,
        IROpCode.CMP_LLT, IROpCode.CMP_LGE,
        IROpCode.CMP_LGE, IROpCode.CMP_LLT,
        IROpCode.CMP_LGT, IROpCode.CMP_LLE,
        IROpCode.CMP_LLE, IROpCode.CMP_LGT
    );

    /**
     * 获取给定比较操作的“相反操作码”。
     */
    public static IROpCode invert(IROpCode op) {
        return INVERT.get(op);
    }
}
