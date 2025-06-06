package org.jcnc.snow.compiler.ir.utils;

import org.jcnc.snow.compiler.ir.core.IROpCode;

import java.util.Map;

/**
 * IR 操作码辅助工具。
 */
public class IROpCodeUtils {
    private static final Map<IROpCode, IROpCode> INVERT = Map.of(
        IROpCode.CMP_EQ, IROpCode.CMP_NE,
        IROpCode.CMP_NE, IROpCode.CMP_EQ,
        IROpCode.CMP_LT, IROpCode.CMP_GE,
        IROpCode.CMP_GE, IROpCode.CMP_LT,
        IROpCode.CMP_GT, IROpCode.CMP_LE,
        IROpCode.CMP_LE, IROpCode.CMP_GT
    );

    /**
     * 获取给定比较操作的“相反操作码”。
     */
    public static IROpCode invert(IROpCode op) {
        return INVERT.get(op);
    }
}
