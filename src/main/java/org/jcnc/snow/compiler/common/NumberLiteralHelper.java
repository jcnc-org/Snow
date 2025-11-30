package org.jcnc.snow.compiler.common;

/**
 * 数字字面量的通用辅助工具。
 * <p>
 * 负责处理下划线清理、类型后缀识别以及进制判定，避免各阶段重复实现。
 */
public final class NumberLiteralHelper {

    private NumberLiteralHelper() {
    }

    /**
     * 规整数字字面量，返回去掉可选后缀/下划线后的文本以及进制信息。
     *
     * @param raw         原始字面量
     * @param stripSuffix 是否移除末尾的类型后缀（若存在）
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

        if (stripSuffix) {
            char suffix = extractTypeSuffix(t);
            if (suffix != '\0') {
                t = t.substring(0, t.length() - 1);
            }
        }

        t = t.replace("_", "");
        return new NormalizedLiteral(t, isHex ? 16 : 10);
    }

    /**
     * 判断文本是否为 16 进制字面量（以 0x/0X 开头）。
     */
    public static boolean isHexLiteral(String raw) {
        if (raw == null) return false;
        String t = raw.trim();
        return t.length() > 2 && t.charAt(0) == '0'
                && (t.charAt(1) == 'x' || t.charAt(1) == 'X');
    }

    /**
     * 返回小写的类型后缀字符；若不存在或末尾字符属于 16 进制数字，则返回 '\0'。
     */
    public static char extractTypeSuffix(String raw) {
        if (raw == null || raw.isEmpty()) return '\0';
        String t = raw.trim();
        if (t.isEmpty()) return '\0';
        char last = t.charAt(t.length() - 1);
        if (!Character.isLetter(last)) return '\0';
        if (isHexLiteral(t) && isHexDigit(last)) return '\0';
        return Character.toLowerCase(last);
    }

    /**
     * 判断单个字符是否为 16 进制数字。
     */
    public static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9')
                || (c >= 'a' && c <= 'f')
                || (c >= 'A' && c <= 'F');
    }

    /**
     * 判断文本层面是否像浮点数（包含 '.' 或 e/E），16 进制字面量始终视为整型。
     */
    public static boolean looksLikeFloat(String raw) {
        NormalizedLiteral n = normalize(raw, false);
        if (n.isHex()) return false;
        String t = n.text();
        return t.indexOf('.') >= 0 || t.indexOf('e') >= 0 || t.indexOf('E') >= 0;
    }

    /**
     * 规整后的数字字面量。
     *
     * @param text  去掉可选后缀与下划线后的文本（保留 0x/0X 前缀）
     * @param radix 进制（10 或 16）
     */
    public record NormalizedLiteral(String text, int radix) {
        public boolean isHex() {
            return radix == 16;
        }

        /**
         * 去掉 0x/0X 前缀后的纯数字部分。
         */
        public String digits() {
            return isHex() && text.length() >= 2 ? text.substring(2) : text;
        }
    }

    public static int parseIntLiteral(String digits, int radix) {
        if (radix == 16) {
            return Integer.parseUnsignedInt(digits, radix);
        }
        return Integer.parseInt(digits);
    }

    public static long parseLongLiteral(String digits, int radix) {
        if (radix == 16) {
            return Long.parseUnsignedLong(digits, radix);
        }
        return Long.parseLong(digits);
    }
}