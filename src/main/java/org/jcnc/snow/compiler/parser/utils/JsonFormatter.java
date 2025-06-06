package org.jcnc.snow.compiler.parser.utils;

/**
 * JSON 格式化工具类。
 * 提供将紧凑 JSON 字符串美化为带缩进和换行的易读格式的方法。
 */
public class JsonFormatter {

    /**
     * 对一个紧凑的 JSON 字符串进行缩进美化。
     * 例如：
     * <pre>{@code
     * {"a":1,"b":[2,3]} →
     * {
     *   "a": 1,
     *   "b": [
     *     2,
     *     3
     *   ]
     * }
     * }</pre>
     *
     * @param json 紧凑的 JSON 字符串。
     * @return 格式化后的 JSON 字符串，带有缩进与换行。
     */
    public static String prettyPrint(String json) {
        StringBuilder sb = new StringBuilder();
        int indent = 0;
        boolean inQuotes = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            // 检查是否进入或退出字符串（忽略转义的引号）
            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
                sb.append(c);
            } else if (!inQuotes) {
                switch (c) {
                    case '{', '[' -> {
                        sb.append(c).append('\n');
                        indent++;
                        appendIndent(sb, indent);
                    }
                    case '}', ']' -> {
                        sb.append('\n');
                        indent--;
                        appendIndent(sb, indent);
                        sb.append(c);
                    }
                    case ',' -> sb.append(c).append('\n').append("  ".repeat(indent));
                    case ':' -> sb.append(c).append(' ');
                    default -> {
                        if (!Character.isWhitespace(c)) {
                            sb.append(c);
                        }
                    }
                }
            } else {
                // 字符串内部原样输出
                sb.append(c);
            }
        }

        return sb.toString();
    }

    /**
     * 向字符串构建器追加指定层级的缩进。
     *
     * @param sb     输出目标。
     * @param indent 缩进层级（每层为两个空格）。
     */
    private static void appendIndent(StringBuilder sb, int indent) {
        sb.append("  ".repeat(Math.max(0, indent)));
    }
}