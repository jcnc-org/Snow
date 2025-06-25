package org.jcnc.snow.compiler.backend.util;

import org.jcnc.snow.vm.engine.VMOpCode;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Opcode 帮助类
 * <p>
 * 通过 <strong>静态不可变 Map</strong> 保存指令名到 opcode（以字符串表示）的映射表。
 * <p>
 * 本文件由脚本根据 {@link VMOpCode} 中实际存在的 <code>public static</code> 字段自动生成，
 * 保证与指令集保持同步，避免手写出错。
 * </p>
 */
public final class OpHelper {

    /**
     * 指令名 → opcode 字符串 的静态映射表
     */
    private static final Map<String, String> OPCODE_MAP;

    /**
     * opcode 字符串 → 指令名 的静态映射表
     */
    private static final Map<Integer, String> OPCODE_NAME_MAP;

    static {
        Map<String, String> map = new HashMap<>();
        map.put("I_ADD", Integer.toString(VMOpCode.I_ADD));
        map.put("I_SUB", Integer.toString(VMOpCode.I_SUB));
        map.put("I_MUL", Integer.toString(VMOpCode.I_MUL));
        map.put("I_DIV", Integer.toString(VMOpCode.I_DIV));
        map.put("I_MOD", Integer.toString(VMOpCode.I_MOD));
        map.put("I_INC", Integer.toString(VMOpCode.I_INC));
        map.put("I_NEG", Integer.toString(VMOpCode.I_NEG));
        map.put("L_ADD", Integer.toString(VMOpCode.L_ADD));
        map.put("L_SUB", Integer.toString(VMOpCode.L_SUB));
        map.put("L_MUL", Integer.toString(VMOpCode.L_MUL));
        map.put("L_DIV", Integer.toString(VMOpCode.L_DIV));
        map.put("L_MOD", Integer.toString(VMOpCode.L_MOD));
        map.put("L_INC", Integer.toString(VMOpCode.L_INC));
        map.put("L_NEG", Integer.toString(VMOpCode.L_NEG));
        map.put("S_ADD", Integer.toString(VMOpCode.S_ADD));
        map.put("S_SUB", Integer.toString(VMOpCode.S_SUB));
        map.put("S_MUL", Integer.toString(VMOpCode.S_MUL));
        map.put("S_DIV", Integer.toString(VMOpCode.S_DIV));
        map.put("S_MOD", Integer.toString(VMOpCode.S_MOD));
        map.put("S_INC", Integer.toString(VMOpCode.S_INC));
        map.put("S_NEG", Integer.toString(VMOpCode.S_NEG));
        map.put("B_ADD", Integer.toString(VMOpCode.B_ADD));
        map.put("B_SUB", Integer.toString(VMOpCode.B_SUB));
        map.put("B_MUL", Integer.toString(VMOpCode.B_MUL));
        map.put("B_DIV", Integer.toString(VMOpCode.B_DIV));
        map.put("B_MOD", Integer.toString(VMOpCode.B_MOD));
        map.put("B_INC", Integer.toString(VMOpCode.B_INC));
        map.put("B_NEG", Integer.toString(VMOpCode.B_NEG));
        map.put("D_ADD", Integer.toString(VMOpCode.D_ADD));
        map.put("D_SUB", Integer.toString(VMOpCode.D_SUB));
        map.put("D_MUL", Integer.toString(VMOpCode.D_MUL));
        map.put("D_DIV", Integer.toString(VMOpCode.D_DIV));
        map.put("D_MOD", Integer.toString(VMOpCode.D_MOD));
        map.put("D_NEG", Integer.toString(VMOpCode.D_NEG));
        map.put("F_ADD", Integer.toString(VMOpCode.F_ADD));
        map.put("F_SUB", Integer.toString(VMOpCode.F_SUB));
        map.put("F_MUL", Integer.toString(VMOpCode.F_MUL));
        map.put("F_DIV", Integer.toString(VMOpCode.F_DIV));
        map.put("F_MOD", Integer.toString(VMOpCode.F_MOD));
        map.put("F_NEG", Integer.toString(VMOpCode.F_NEG));
        map.put("D_INC", Integer.toString(VMOpCode.D_INC));
        map.put("F_INC", Integer.toString(VMOpCode.F_INC));
        map.put("I2L", Integer.toString(VMOpCode.I2L));
        map.put("I2S", Integer.toString(VMOpCode.I2S));
        map.put("I2B", Integer.toString(VMOpCode.I2B));
        map.put("I2D", Integer.toString(VMOpCode.I2D));
        map.put("I2F", Integer.toString(VMOpCode.I2F));
        map.put("L2I", Integer.toString(VMOpCode.L2I));
        map.put("L2D", Integer.toString(VMOpCode.L2D));
        map.put("L2F", Integer.toString(VMOpCode.L2F));
        map.put("F2I", Integer.toString(VMOpCode.F2I));
        map.put("F2L", Integer.toString(VMOpCode.F2L));
        map.put("F2D", Integer.toString(VMOpCode.F2D));
        map.put("D2I", Integer.toString(VMOpCode.D2I));
        map.put("D2L", Integer.toString(VMOpCode.D2L));
        map.put("D2F", Integer.toString(VMOpCode.D2F));
        map.put("S2I", Integer.toString(VMOpCode.S2I));
        map.put("B2I", Integer.toString(VMOpCode.B2I));
        map.put("I_AND", Integer.toString(VMOpCode.I_AND));
        map.put("I_OR", Integer.toString(VMOpCode.I_OR));
        map.put("I_XOR", Integer.toString(VMOpCode.I_XOR));
        map.put("L_AND", Integer.toString(VMOpCode.L_AND));
        map.put("L_OR", Integer.toString(VMOpCode.L_OR));
        map.put("L_XOR", Integer.toString(VMOpCode.L_XOR));
        map.put("JUMP", Integer.toString(VMOpCode.JUMP));
        map.put("IC_E", Integer.toString(VMOpCode.IC_E));
        map.put("IC_NE", Integer.toString(VMOpCode.IC_NE));
        map.put("IC_G", Integer.toString(VMOpCode.IC_G));
        map.put("IC_GE", Integer.toString(VMOpCode.IC_GE));
        map.put("IC_L", Integer.toString(VMOpCode.IC_L));
        map.put("IC_LE", Integer.toString(VMOpCode.IC_LE));
        map.put("LC_E", Integer.toString(VMOpCode.LC_E));
        map.put("LC_NE", Integer.toString(VMOpCode.LC_NE));
        map.put("LC_G", Integer.toString(VMOpCode.LC_G));
        map.put("LC_GE", Integer.toString(VMOpCode.LC_GE));
        map.put("LC_L", Integer.toString(VMOpCode.LC_L));
        map.put("LC_LE", Integer.toString(VMOpCode.LC_LE));
        map.put("I_PUSH", Integer.toString(VMOpCode.I_PUSH));
        map.put("L_PUSH", Integer.toString(VMOpCode.L_PUSH));
        map.put("S_PUSH", Integer.toString(VMOpCode.S_PUSH));
        map.put("B_PUSH", Integer.toString(VMOpCode.B_PUSH));
        map.put("D_PUSH", Integer.toString(VMOpCode.D_PUSH));
        map.put("F_PUSH", Integer.toString(VMOpCode.F_PUSH));
        map.put("POP", Integer.toString(VMOpCode.POP));
        map.put("DUP", Integer.toString(VMOpCode.DUP));
        map.put("SWAP", Integer.toString(VMOpCode.SWAP));
        map.put("I_STORE", Integer.toString(VMOpCode.I_STORE));
        map.put("L_STORE", Integer.toString(VMOpCode.L_STORE));
        map.put("S_STORE", Integer.toString(VMOpCode.S_STORE));
        map.put("B_STORE", Integer.toString(VMOpCode.B_STORE));
        map.put("D_STORE", Integer.toString(VMOpCode.D_STORE));
        map.put("F_STORE", Integer.toString(VMOpCode.F_STORE));
        map.put("I_LOAD", Integer.toString(VMOpCode.I_LOAD));
        map.put("L_LOAD", Integer.toString(VMOpCode.L_LOAD));
        map.put("S_LOAD", Integer.toString(VMOpCode.S_LOAD));
        map.put("B_LOAD", Integer.toString(VMOpCode.B_LOAD));
        map.put("D_LOAD", Integer.toString(VMOpCode.D_LOAD));
        map.put("F_LOAD", Integer.toString(VMOpCode.F_LOAD));
        map.put("MOV", Integer.toString(VMOpCode.MOV));
        map.put("CALL", Integer.toString(VMOpCode.CALL));
        map.put("RET", Integer.toString(VMOpCode.RET));
        map.put("HALT", Integer.toString(VMOpCode.HALT));
        OPCODE_MAP = Collections.unmodifiableMap(map);

        Map<Integer, String> revmap = new HashMap<>();  // reverse map
        OPCODE_MAP.forEach((key, value) -> revmap.put(Integer.parseInt(value), key));

        OPCODE_NAME_MAP = Collections.unmodifiableMap(revmap);
    }

    /**
     * 私有构造器，禁止实例化
     */
    private OpHelper() {
    }

    /**
     * 根据指令名获取 opcode 字符串。
     *
     * @param name 指令名（如 "I_LOAD"、"I_PUSH"）
     * @return opcode 字符串
     * @throws IllegalStateException 若名称未知
     */
    public static String opcode(String name) {
        String code = OPCODE_MAP.get(name);
        if (code == null) {
            throw new IllegalStateException("Unknown opcode: " + name);
        }
        return code;
    }

    // region 辅助方法 – 根据常量类型推导 PUSH / STORE 指令名

    public static String pushOpcodeFor(Object v) {
        return opcode(typePrefix(v) + "_PUSH");
    }

    public static String storeOpcodeFor(Object v) {
        return opcode(typePrefix(v) + "_STORE");
    }

    private static String typePrefix(Object v) {
        if (v instanceof Integer) return "I";
        if (v instanceof Long) return "L";
        if (v instanceof Short) return "S";
        if (v instanceof Byte) return "B";
        if (v instanceof Double) return "D";
        if (v instanceof Float) return "F";
        throw new IllegalStateException("Unknown const type: " + v.getClass());
    }

    /**
     * 根据 opcode 数值的字符串形式获取指令名
     * @param code 字符串形式的 opcode 数值
     * @return opcode 对应的指令名
     */
    public static String opcodeName(String code) {
        return opcodeName(Integer.parseInt(code));
    }

    /**
     * 根据 opcode 获取指令名
     * @param code opcode
     * @return opcode 对应的指令名
     */
    public static String opcodeName(int code) {
        String name = OPCODE_NAME_MAP.get(code);
        if (name == null) {
            throw new IllegalStateException("Unknown opcode: " + name);
        }
        return name;
    }

    // endregion
}