package org.jcnc.snow.compiler.backend.utils;

/**
 * 工具类: 提供基本数值类型的提升与类型转换辅助功能。
 * <p>
 * 在进行数值类型运算、比较等操作时，低优先级的类型会被提升为高优先级类型参与运算。
 * 例如 int + long 运算，int 会被提升为 long，最终运算结果类型为 long。
 * <p>
 * 支持的类型标记如下（优先级从高到低）：
 * <ul>
 *   <li>'R' : 引用类型（如字符串），优先级最高（7）</li>
 *   <li>'D' : double 类型，优先级 6</li>
 *   <li>'F' : float 类型，优先级 5</li>
 *   <li>'L' : long 类型，优先级 4</li>
 *   <li>'I' : int 类型，优先级 3</li>
 *   <li>'S' : short 类型，优先级 2</li>
 *   <li>'B' : byte 类型，优先级 1</li>
 *   <li>未知类型，优先级 0</li>
 * </ul>
 * </p>
 */
public class TypePromoteUtils {

    /**
     * 获取类型标记的优先级数值。
     * <p>
     * 类型及优先级对应关系：
     * <ul>
     *   <li>'R' - 7 （引用类型，字符串等）</li>
     *   <li>'D' - 6</li>
     *   <li>'F' - 5</li>
     *   <li>'L' - 4</li>
     *   <li>'I' - 3</li>
     *   <li>'S' - 2</li>
     *   <li>'B' - 1</li>
     *   <li>其他 - 0</li>
     * </ul>
     * </p>
     *
     * @param p 类型标记字符（B/S/I/L/F/D/R）
     * @return 该类型的宽度优先级，数值越大类型越宽。未知类型返回 0。
     */
    private static int rank(char p) {
        return switch (p) {
            case 'R' -> 7;
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
     * 计算两个类型标记字符的公共类型（即较宽的类型）。
     * <p>
     * 类型提升规则如下：
     * <ul>
     *   <li>若任一类型为 'R'（引用类型），则返回 'R'</li>
     *   <li>否则，返回优先级较高的类型标记</li>
     *   <li>若优先级相等，返回第一个参数的类型标记</li>
     * </ul>
     * </p>
     *
     * @param a 类型标记字符1（如 'I'、'L'、'D' 等）
     * @param b 类型标记字符2
     * @return 两者中较“宽”的类型标记字符（如 'L'）
     */
    public static char promote(char a, char b) {
        if (a == 'R' || b == 'R') {
            return 'R';
        }
        return rank(a) >= rank(b) ? a : b;
    }

    /**
     * 将类型标记字符转换为字符串形式。
     * <p>
     * 常用于类型描述、调试输出等场景。
     * </p>
     *
     * @param p 类型标记字符（如 'I', 'L', 'D' 等）
     * @return 字符串形式的类型标记
     */
    public static String str(char p) {
        return String.valueOf(p);
    }

    /**
     * 获取类型转换指令（JVM 层面）的字符串名称，如 "I2L" 表示 int 转 long。
     * <p>
     * 如果源类型与目标类型相同，则返回 null（表示无需转换）。
     * 支持以下基本类型标记的两两转换：
     * <ul>
     *   <li>B（byte）</li>
     *   <li>S（short）</li>
     *   <li>I（int）</li>
     *   <li>L（long）</li>
     *   <li>F（float）</li>
     *   <li>D（double）</li>
     * </ul>
     * 转换对照关系如下：
     * <ul>
     *   <li>B → S/I/L/F/D</li>
     *   <li>S → B/I/L/F/D</li>
     *   <li>I → B/S/L/F/D</li>
     *   <li>L → B/S/I/F/D</li>
     *   <li>F → B/S/I/L/D</li>
     *   <li>D → B/S/I/L/F</li>
     * </ul>
     * </p>
     *
     * @param from 源类型标记字符
     * @param to   目标类型标记字符
     * @return 类型转换指令字符串（如 "I2L"），如果无需转换则返回 null
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
