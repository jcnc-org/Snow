package org.jcnc.snow.common;

/**
 * 数字字面量处理工具类。
 *
 * <p>
 * 用于对数字字面量执行统一的预处理工作，包括：
 * </p>
 * <ul>
 *     <li>去除下划线分隔符</li>
 *     <li>识别并提取类型后缀（如 L、F、D 等）</li>
 *     <li>判断字面量进制（十进制/十六进制）</li>
 *     <li>提供统一的规整结构 {@link NormalizedLiteral}</li>
 * </ul>
 *
 * <p>
 * 该类在编译流程的多个阶段被使用，避免在各个组件中重复实现字面量处理逻辑。
 * </p>
 */
public final class NumberLiteralHelper {

    private NumberLiteralHelper() {
    }

    /**
     * 规整（Normalize）数字字面量，返回去除下划线、可选去除类型后缀后得到的文本，
     * 并判定其进制（十进制或十六进制）。
     *
     * @param raw         原始数字字面量
     * @param stripSuffix 是否移除末尾类型后缀（若存在且为字母）
     * @return 规整后的字面量结构 {@link NormalizedLiteral}
     */
    public static NormalizedLiteral normalize(String raw, boolean stripSuffix) {
        if (raw == null) {
            return new NormalizedLiteral("", 10);
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return new NormalizedLiteral(t, 10);
        }

        boolean isHex = isHexLiteral(t);

        // 去除末尾类型后缀
        if (stripSuffix) {
            char suffix = extractTypeSuffix(t);
            if (suffix != '\0') {
                t = t.substring(0, t.length() - 1);
            }
        }

        // 去除下划线
        t = t.replace("_", "");
        return new NormalizedLiteral(t, isHex ? 16 : 10);
    }

    /**
     * 判断字面量是否为十六进制形式（以 0x 或 0X 开头）。
     *
     * @param raw 原始字面量
     * @return 若为十六进制字面量返回 true，否则返回 false
     */
    public static boolean isHexLiteral(String raw) {
        if (raw == null) return false;
        String t = raw.trim();
        return t.length() > 2
                && t.charAt(0) == '0'
                && (t.charAt(1) == 'x' || t.charAt(1) == 'X');
    }

    /**
     * 提取数字字面量末尾的类型后缀字符。
     *
     * <p>说明：</p>
     * <ul>
     *     <li>若末尾非字母则无后缀</li>
     *     <li>十六进制字面量末尾若是合法十六进制字符（0–9, a–f），则视为数字而非后缀</li>
     * </ul>
     *
     * @param raw 原始字面量
     * @return 类型后缀字符（小写），若无则返回 '\0'
     */
    public static char extractTypeSuffix(String raw) {
        if (raw == null || raw.isEmpty()) return '\0';
        String t = raw.trim();
        if (t.isEmpty()) return '\0';

        char last = t.charAt(t.length() - 1);
        if (!Character.isLetter(last)) return '\0';

        // 十六进制字面量末尾若是数字有效字符，则不视为后缀
        if (isHexLiteral(t) && isHexDigit(last)) return '\0';

        return Character.toLowerCase(last);
    }

    /**
     * 判断字符是否为十六进制数字（0–9 或 a–f 或 A–F）。
     *
     * @param c 字符
     * @return 若为十六进制数字返回 true
     */
    public static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9')
                || (c >= 'a' && c <= 'f')
                || (c >= 'A' && c <= 'F');
    }

    /**
     * 判断字面量文本是否“看起来像”浮点数。
     *
     * <p>判断规则：</p>
     * <ul>
     *     <li>包含 {@code '.'} 或 {@code e/E} 即视为浮点数</li>
     *     <li>十六进制字面量一律视为整型字面量</li>
     * </ul>
     *
     * @param raw 原始字面量
     * @return 若判断为浮点形式返回 true
     */
    public static boolean looksLikeFloat(String raw) {
        NormalizedLiteral n = normalize(raw, false);
        if (n.isHex()) return false;
        String t = n.text();
        return t.indexOf('.') >= 0 || t.indexOf('e') >= 0 || t.indexOf('E') >= 0;
    }

    /**
     * 表示规整后的字面量。
     *
     * @param text  去除下划线与可选类型后缀后的文本（保留 0x/0X 前缀）
     * @param radix 数值进制（10 或 16）
     */
    public record NormalizedLiteral(String text, int radix) {

        /**
         * 是否为十六进制字面量。
         */
        public boolean isHex() {
            return radix == 16;
        }

        /**
         * 获取纯数字部分（若为十六进制则去掉 0x/0X）。
         *
         * @return 不带前缀的数字部分
         */
        public String digits() {
            return isHex() && text.length() >= 2 ? text.substring(2) : text;
        }
    }

    /**
     * 将规整后的十进制或十六进制数字转换为 int。
     *
     * @param digits 不包含前缀的数字文本
     * @param radix  进制
     * @return 对应的 int 值
     */
    public static int parseIntLiteral(String digits, int radix) {
        if (radix == 16) {
            return Integer.parseUnsignedInt(digits, radix);
        }
        return Integer.parseInt(digits);
    }

    /**
     * 将规整后的十进制或十六进制数字转换为 long。
     *
     * @param digits 不包含前缀的数字文本
     * @param radix  进制
     * @return 对应的 long 值
     */
    public static long parseLongLiteral(String digits, int radix) {
        if (radix == 16) {
            return Long.parseUnsignedLong(digits, radix);
        }
        return Long.parseLong(digits);
    }
}