package org.jcnc.snow.compiler.backend.utils;

/**
 * 工具类：提供基本数值类型的提升与类型转换辅助功能。
 * <p>
 * 在进行数值类型运算、比较等操作时，低优先级的类型会被提升为高优先级类型参与运算。
 * 例如 int + long 运算，int 会被提升为 long，最终运算结果类型为 long。
 * <p>
 * 类型优先级从高到低依次为：
 * D（double）：6
 * F（float） ：5
 * L（long）  ：4
 * I（int）   ：3
 * S（short） ：2
 * B（byte）  ：1
 * 未识别类型 ：0
 */
public class TypePromoteUtils {

    /**
     * 返回数值类型的宽度优先级，数值越大类型越宽。
     * 类型及优先级映射如下：
     * D（double）: 6
     * F（float） : 5
     * L（long）  : 4
     * I（int）   : 3
     * S（short） : 2
     * B（byte）  : 1
     * 未知类型    : 0
     *
     * @param p 类型标记字符（B/S/I/L/F/D）
     * @return 优先级数值（0 表示未知类型）
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
     * 返回两个类型中较“宽”的公共类型（即优先级较高的类型）。
     * 若优先级相等，返回第一个参数的类型。
     *
     * @param a 类型标记字符1
     * @param b 类型标记字符2
     * @return 优先级较高的类型标记字符
     */
    public static char promote(char a, char b) {
        return rank(a) >= rank(b) ? a : b;
    }

    /**
     * 将单个字符的类型标记转为字符串。
     *
     * @param p 类型标记字符
     * @return 类型标记的字符串形式
     */
    public static String str(char p) {
        return String.valueOf(p);
    }

    /**
     * 获取类型转换指令名（例如 "I2L", "F2D"），表示从源类型到目标类型的转换操作。
     * 如果源类型和目标类型相同，则返回 null，表示无需转换。
     * <p>
     * 支持的类型标记字符包括：B（byte）、S（short）、I（int）、L（long）、F（float）、D（double）。
     * 所有可能的类型转换均已覆盖，如下所示：
     * B → S/I/L/F/D
     * S → B/I/L/F/D
     * I → B/S/L/F/D
     * L → B/S/I/F/D
     * F → B/S/I/L/D
     * D → B/S/I/L/F
     *
     * @param from 源类型标记字符
     * @param to   目标类型标记字符
     * @return 类型转换指令名（如 "L2I"），如无须转换则返回 null
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
