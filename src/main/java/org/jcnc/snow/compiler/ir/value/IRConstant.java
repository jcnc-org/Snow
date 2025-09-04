package org.jcnc.snow.compiler.ir.value;

import org.jcnc.snow.compiler.ir.core.IRValue;

/**
 * {@code IRConstant} —— 表示中间表示（IR）系统中的常量值节点。
 * <p>
 * 常量用于表示在编译期间已知的不可变值，例如字面整数、浮点数、布尔值或字符串等。
 * 与 {@link IRVirtualRegister} 不同，IRConstant 不需要通过寄存器存储，
 * 可直接作为 IR 指令的操作数（立即数/常量池引用等）。
 */
public record IRConstant(Object value) implements IRValue {

    /**
     * 通用工厂方法：将任意 Java 对象包装为 IRConstant。
     *
     * @param v 任意对象
     * @return IRConstant 封装后的常量
     */
    public static IRConstant fromObject(Object v) {
        return new IRConstant(v);
    }

    /**
     * 数字字面量工厂：接受源码字面量字符串，解析为 int/long/double 等 Java 数值类型。
     * <p>
     * 支持下划线分隔（如 "1_000_000"）、类型后缀（如 'l','d','f' 等），并自动转换为
     * <ul>
     *   <li>double：带小数点或科学计数法</li>
     *   <li>long/byte/short/int：依类型后缀或值范围判定</li>
     *   <li>解析失败时兜底为字符串原文，保证编译不中断</li>
     * </ul>
     *
     * @param literal 字面量字符串（可能含类型后缀、下划线分隔）
     * @return IRConstant 包装的数字常量
     */
    public static IRConstant fromNumber(String literal) {
        if (literal == null) return new IRConstant(null);
        String s = literal.replace("_", ""); // 去除下划线分隔符
        // 检查并处理类型后缀（b/s/l/f/d）
        char last = Character.toLowerCase(s.charAt(s.length() - 1));
        String core = switch (last) {
            case 'b', 's', 'l', 'f', 'd' -> s.substring(0, s.length() - 1);
            default -> s;
        };
        try {
            // 浮点：含小数点或科学计数法
            if (core.contains(".") || core.contains("e") || core.contains("E")) {
                return new IRConstant(Double.parseDouble(core));
            }
            long lv = Long.parseLong(core); // 整型
            return switch (last) {
                case 'b' -> new IRConstant((byte) lv);    // 字节型
                case 's' -> new IRConstant((short) lv);   // 短整型
                case 'l' -> new IRConstant(lv);           // 长整型
                default -> new IRConstant((int) lv);      // 默认 int
            };
        } catch (NumberFormatException e) {
            // 解析失败时，回退为原始字符串，避免编译中断
            return new IRConstant(core);
        }
    }

    /**
     * 字符串字面量工厂。
     *
     * @param s 字符串内容
     * @return IRConstant 封装的字符串常量
     */
    public static IRConstant fromString(String s) {
        return new IRConstant(s);
    }

    /**
     * 布尔字面量工厂。
     *
     * @param v 布尔值
     * @return IRConstant 封装的布尔常量
     */
    public static IRConstant fromBoolean(boolean v) {
        return new IRConstant(v);
    }

    /**
     * 调试友好的字符串表示。
     * <ul>
     *   <li>字符串常量输出带引号，转义引号和反斜杠</li>
     *   <li>null 输出为 "null"</li>
     *   <li>其它类型调用 toString()</li>
     * </ul>
     */
    @Override
    public String toString() {
        if (value == null) return "null";
        if (value instanceof String s) {
            String escaped = s.replace("\\", "\\\\").replace("\"", "\\\"");
            return "\"" + escaped + "\"";
        }
        return String.valueOf(value);
    }
}
