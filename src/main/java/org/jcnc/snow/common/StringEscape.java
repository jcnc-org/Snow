package org.jcnc.snow.common;

/**
 * <p>
 * 字符串转义/反转义工具类，主要用于：
 * <ul>
 *     <li><b>编译期</b>：将运行时的字符串安全地编码为单行形式（用于 .water 指令文件的保存）。</li>
 *     <li><b>运行期</b>：在虚拟机（VM）执行相关指令时，将转义后的字符串还原成真实字符。</li>
 * </ul>
 * <br>
 * 转义规则兼容 Java 字符串转义（包括 \n, \t, \r 等常见控制字符），同时对于不可见或非 ASCII 字符，会编码为 Unicode 形式（如 <code>uXXXX</code>）。
 * </p>
 */
public final class StringEscape {

    /**
     * 工具类私有构造方法，禁止实例化。
     */
    private StringEscape() {
    }

    /**
     * <b>编译期方法：</b>
     * <p>将普通字符串编码为可安全存储于单行文本（如 .water 指令）的转义字符串。</p>
     *
     * <ul>
     *     <li>常见控制字符（如换行、制表符、回车等）会转为转义序列（如 <code>\n</code>、<code>\t</code>）。</li>
     *     <li>反斜杠、单双引号等特殊字符会转为对应转义形式。</li>
     *     <li>非可见 ASCII 字符或超出 0x7E 范围的字符，会被编码为 Unicode 形式 <code>uXXXX</code>。</li>
     * </ul>
     *
     * @param input 原始字符串（可能包含特殊/不可见字符）
     * @return 转义后的字符串，可安全作为单行文本保存
     */
    public static String escape(String input) {
        StringBuilder sb = new StringBuilder();
        for (char ch : input.toCharArray()) {
            switch (ch) {
                case '\n' -> sb.append("\\n");         // 换行
                case '\t' -> sb.append("\\t");         // 制表符
                case '\r' -> sb.append("\\r");         // 回车
                case '\b' -> sb.append("\\b");         // 退格
                case '\f' -> sb.append("\\f");         // 换页
                case '\\' -> sb.append("\\\\");        // 反斜杠
                case '"' -> sb.append("\\\"");        // 双引号
                case '\'' -> sb.append("\\'");         // 单引号
                default -> {
                    // 对于不可见的 ASCII 字符（<0x20）或非 ASCII 可见字符（>0x7E），转为 Unicode
                    if (ch < 0x20 || ch > 0x7E) {
                        sb.append(String.format("\\u%04X", (int) ch));
                    } else {
                        sb.append(ch);                 // 其他字符直接附加
                    }
                }
            }
        }
        return sb.toString();
    }

    /**
     * <b>运行期方法：</b>
     * <p>将转义序列（如 <code>\n</code>, <code>\u4E16</code> 等）还原为实际字符。</p>
     *
     * <ul>
     *     <li>支持常见的转义字符序列。</li>
     *     <li>支持 uXXXX 形式的 Unicode 字符反转义。</li>
     *     <li>对于无法识别的转义，按原样输出。</li>
     * </ul>
     *
     * @param src 含有转义序列的字符串
     * @return 反转义后的字符串，原样还原
     */
    public static String unescape(String src) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < src.length(); i++) {
            char c = src.charAt(i);
            if (c != '\\') {            // 非转义字符，直接输出
                out.append(c);
                continue;
            }

            // 如果是最后一个字符为反斜杠，则原样输出
            if (i == src.length() - 1) {
                out.append('\\');
                break;
            }

            char n = src.charAt(++i);   // 下一个字符
            switch (n) {
                case 'n' -> out.append('\n');     // 换行
                case 't' -> out.append('\t');     // 制表符
                case 'r' -> out.append('\r');     // 回车
                case 'b' -> out.append('\b');     // 退格
                case 'f' -> out.append('\f');     // 换页
                case '\\' -> out.append('\\');     // 反斜杠
                case '"' -> out.append('"');      // 双引号
                case '\'' -> out.append('\'');     // 单引号
                case 'u' -> {
                    // Unicode 转义，需读取接下来的 4 位十六进制数字
                    if (i + 4 <= src.length() - 1) {
                        String hex = src.substring(i + 1, i + 5);
                        try {
                            out.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        } catch (NumberFormatException ignore) {
                            // 非法 hex，原样输出
                            out.append("\\u").append(hex);
                            i += 4;
                        }
                    } else {
                        // 字符串末尾长度不足，原样输出
                        out.append("\\u");
                    }
                }
                default -> out.append(n);         // 其他未定义的转义序列，原样输出
            }
        }
        return out.toString();
    }
}
