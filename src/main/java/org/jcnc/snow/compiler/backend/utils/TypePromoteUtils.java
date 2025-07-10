package org.jcnc.snow.compiler.backend.utils;

/**
 * 基本数值类型提升工具：
 * 类型优先级低的类型转为优先级高的类型，去参与比较、计算等等,
 * 比如 int + long，那么结果的类型应为 long
 */
public class TypePromoteUtils {
    /**
     * <b>类型宽度优先级</b>：D > F > L > I > S > B
     * <ul>
     *     <li>D（double）：6</li>
     *     <li>F（float）：5</li>
     *     <li>L（long）：4</li>
     *     <li>I（int）：3</li>
     *     <li>S（short）：2</li>
     *     <li>B（byte）：1</li>
     *     <li>未识别类型：0</li>
     * </ul>
     *
     * @param p 类型标记字符
     * @return 优先级数值（越大类型越宽）
     */
    private static int rank(char p) {
        return switch (p) {
            case 'D' -> 6;
            case 'F' -> 5;
            case 'L' -> 4;
            case 'I' -> 3;
            case 'S' -> 2;
            case 'B' -> 1;
            default -> 0;
        };
    }

    /**
     * 返回更“宽”的公共类型（即优先级高的类型）。
     *
     * @param a 类型标记字符 1
     * @param b 类型标记字符 2
     * @return 宽度更高的类型标记字符
     */
    public static char promote(char a, char b) {
        return rank(a) >= rank(b) ? a : b;
    }

    /**
     * 单字符类型标记转字符串。
     *
     * @param p 类型标记字符
     * @return 类型字符串
     */
    public static String str(char p) {
        return String.valueOf(p);
    }

    /**
     * 获取 {@code from → to} 的类型转换指令名（如不需转换则返回 {@code null}）。
     *
     * @param from 源类型标记字符
     * @param to   目标类型标记字符
     * @return 转换指令名，如“L2I”；无转换返回 {@code null}
     */
    public static String convert(char from, char to) {
        if (from == to) return null;
        return switch ("" + from + to) {
            case "BS" -> "B2S";
            case "BI" -> "B2I";
            case "BL" -> "B2L";
            case "BF" -> "B2F";
            case "BD" -> "B2D";

            case "SB" -> "S2B";
            case "SI" -> "S2I";
            case "SL" -> "S2L";
            case "SF" -> "S2F";
            case "SD" -> "S2D";

            case "IB" -> "I2B";
            case "IS" -> "I2S";
            case "IL" -> "I2L";
            case "IF" -> "I2F";
            case "ID" -> "I2D";

            case "LB" -> "L2B";
            case "LS" -> "L2S";
            case "LI" -> "L2I";
            case "LF" -> "L2F";
            case "LD" -> "L2D";

            case "FB" -> "F2B";
            case "FS" -> "F2S";
            case "FI" -> "F2I";
            case "FL" -> "F2L";
            case "FD" -> "F2D";

            case "DB" -> "D2B";
            case "DS" -> "D2S";
            case "DI" -> "D2I";
            case "DL" -> "D2L";
            case "DF" -> "D2F";
            default -> null;
        };
    }
}
