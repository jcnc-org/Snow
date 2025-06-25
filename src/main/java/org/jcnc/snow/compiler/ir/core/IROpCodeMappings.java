package org.jcnc.snow.compiler.ir.core;

import java.util.Map;

/**
 * 操作符与IR操作码映射表，统一管理所有类型的算术和比较操作映射。
 */
public final class IROpCodeMappings {
    private IROpCodeMappings() {} // 禁止实例化

    // 8位整型运算符映射
    public static final Map<String, IROpCode> OP_B8 = Map.of(
            "+", IROpCode.ADD_B8, "-", IROpCode.SUB_B8,
            "*", IROpCode.MUL_B8, "/", IROpCode.DIV_B8
    );
    // 16位整型
    public static final Map<String, IROpCode> OP_S16 = Map.of(
            "+", IROpCode.ADD_S16, "-", IROpCode.SUB_S16,
            "*", IROpCode.MUL_S16, "/", IROpCode.DIV_S16
    );
    // 32位整型
    public static final Map<String, IROpCode> OP_I32 = Map.of(
            "+", IROpCode.ADD_I32, "-", IROpCode.SUB_I32,
            "*", IROpCode.MUL_I32, "/", IROpCode.DIV_I32
    );
    // 64位长整型
    public static final Map<String, IROpCode> OP_L64 = Map.of(
            "+", IROpCode.ADD_L64, "-", IROpCode.SUB_L64,
            "*", IROpCode.MUL_L64, "/", IROpCode.DIV_L64
    );
    // 32位浮点型
    public static final Map<String, IROpCode> OP_F32 = Map.of(
            "+", IROpCode.ADD_F32, "-", IROpCode.SUB_F32,
            "*", IROpCode.MUL_F32, "/", IROpCode.DIV_F32
    );
    // 64位双精度浮点型
    public static final Map<String, IROpCode> OP_D64 = Map.of(
            "+", IROpCode.ADD_D64, "-", IROpCode.SUB_D64,
            "*", IROpCode.MUL_D64, "/", IROpCode.DIV_D64
    );

    /* ────── 比较运算符映射 ────── */
    /** 32-bit（int）比较 */
    public static final Map<String, IROpCode> CMP_I32 = Map.of(
            "==", IROpCode.CMP_IEQ,
            "!=", IROpCode.CMP_INE,
            "<",  IROpCode.CMP_ILT,
            ">",  IROpCode.CMP_IGT,
            "<=", IROpCode.CMP_ILE,
            ">=", IROpCode.CMP_IGE
    );

    /** 64-bit（long）比较 */
    public static final Map<String, IROpCode> CMP_L64 = Map.of(
            "==", IROpCode.CMP_LEQ,
            "!=", IROpCode.CMP_LNE,
            "<",  IROpCode.CMP_LLT,
            ">",  IROpCode.CMP_LGT,
            "<=", IROpCode.CMP_LLE,
            ">=", IROpCode.CMP_LGE
    );

    
}
