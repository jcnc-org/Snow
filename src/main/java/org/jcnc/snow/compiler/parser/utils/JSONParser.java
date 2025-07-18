package org.jcnc.snow.compiler.parser.utils;

import org.jcnc.snow.compiler.parser.context.UnexpectedToken;

import java.util.*;
import java.util.Map.Entry;

/**
 * JSON 工具类，提供线程安全、可重用的 JSON 解析与序列化能力。
 * <p>
 * <b>主要功能: </b>
 * <ul>
 *   <li>解析: 将合法的 JSON 文本转换为 Java 原生对象（Map、List、String、Number、Boolean 或 null）</li>
 *   <li>序列化: 将 Java 原生对象转换为符合 JSON 标准的字符串</li>
 * </ul>
 * </p>
 *
 * <b>设计要点: </b>
 * <ol>
 *   <li>仅提供静态方法入口，无状态，线程安全</li>
 *   <li>解析器内部采用 char[] 缓冲区，支持高性能处理</li>
 *   <li>精确维护行列号信息，异常可定位错误文本位置</li>
 *   <li>序列化器使用 StringBuilder，默认预分配容量</li>
 * </ol>
 */
public class JSONParser {

    private JSONParser() {}

    /**
     * 解析 JSON 格式字符串为对应的 Java 对象。
     *
     * @param input JSON 格式字符串
     * @return 解析得到的 Java 对象（Map、List、String、Number、Boolean 或 null）
     * @throws UnexpectedToken 语法错误或多余字符，异常消息带行列定位
     */
    public static Object parse(String input) {
        return new Parser(input).parseInternal();
    }

    /**
     * 将 Java 原生对象序列化为 JSON 字符串。
     *
     * @param obj 支持 Map、Collection、String、Number、Boolean 或 null
     * @return 符合 JSON 规范的字符串
     * @throws UnsupportedOperationException 遇到不支持的类型时抛出
     */
    public static String toJson(Object obj) {
        return Writer.write(obj);
    }

    // ======= 内部解析器实现 =======

    /**
     * 负责将 char[] 缓冲区中的 JSON 文本解析为 Java 对象。
     * 维护行列号，所有异常均带精确位置。
     */
    private static class Parser {
        private final char[] buf;
        private int pos;
        private int line;
        private int col;

        Parser(String input) {
            this.buf = input.toCharArray();
            this.pos = 0;
            this.line = 1;
            this.col = 1;
        }

        /**
         * 解析主入口，校验无多余字符。
         */
        Object parseInternal() {
            skipWhitespace();
            Object value = parseValue();
            skipWhitespace();
            if (pos < buf.length) {
                error("存在无法识别的多余字符");
            }
            return value;
        }

        /**
         * 解析 JSON 值（null, true, false, string, number, object, array）
         */
        private Object parseValue() {
            skipWhitespace();
            if (match("null")) return null;
            if (match("true")) return Boolean.TRUE;
            if (match("false")) return Boolean.FALSE;
            char c = currentChar();
            if (c == '"') return parseString();
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '-' || isDigit(c)) return parseNumber();
            error("遇到意外字符 '" + c + "'");
            return null;
        }

        /**
         * 解析对象类型 { ... }
         */
        private Map<String, Object> parseObject() {
            expect('{');
            skipWhitespace();
            Map<String, Object> map = new LinkedHashMap<>();
            if (currentChar() == '}') {
                advance();
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                Object val = parseValue();
                map.put(key, val);
                skipWhitespace();
                if (currentChar() == '}') {
                    advance();
                    break;
                }
                expect(',');
                skipWhitespace();
            }
            return map;
        }

        /**
         * 解析数组类型 [ ... ]
         */
        private List<Object> parseArray() {
            expect('[');
            skipWhitespace();
            List<Object> list = new ArrayList<>();
            if (currentChar() == ']') {
                advance();
                return list;
            }
            while (true) {
                skipWhitespace();
                list.add(parseValue());
                skipWhitespace();
                if (currentChar() == ']') {
                    advance();
                    break;
                }
                expect(',');
                skipWhitespace();
            }
            return list;
        }

        /**
         * 解析字符串类型，支持标准 JSON 转义。
         */
        private String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = currentChar();
                if (c == '"') {
                    advance();
                    break;
                }
                if (c == '\\') {
                    advance();
                    c = currentChar();
                    switch (c) {
                        case '"':
                            sb.append('"'); break;
                        case '\\':
                            sb.append('\\'); break;
                        case '/':
                            sb.append('/'); break;
                        case 'b':
                            sb.append('\b'); break;
                        case 'f':
                            sb.append('\f'); break;
                        case 'n':
                            sb.append('\n'); break;
                        case 'r':
                            sb.append('\r'); break;
                        case 't':
                            sb.append('\t'); break;
                        case 'u':
                            String hex = new String(buf, pos + 1, 4);
                            sb.append((char) Integer.parseInt(hex, 16));
                            pos += 4;
                            col += 4;
                            break;
                        default:
                            error("无效转义字符 '\\" + c + "'");
                    }
                    advance();
                } else {
                    sb.append(c);
                    advance();
                }
            }
            return sb.toString();
        }

        /**
         * 解析数字类型（支持整数、小数、科学计数法）。
         */
        private Number parseNumber() {
            int start = pos;
            if (currentChar() == '-') advance();
            while (isDigit(currentChar())) advance();
            if (currentChar() == '.') {
                do { advance(); } while (isDigit(currentChar()));
            }
            if (currentChar() == 'e' || currentChar() == 'E') {
                advance();
                if (currentChar() == '+' || currentChar() == '-') advance();
                while (isDigit(currentChar())) advance();
            }
            String num = new String(buf, start, pos - start);
            if (num.indexOf('.') >= 0 || num.indexOf('e') >= 0 || num.indexOf('E') >= 0) {
                return Double.parseDouble(num);
            }
            try {
                return Long.parseLong(num);
            } catch (NumberFormatException e) {
                return Double.parseDouble(num);
            }
        }

        /**
         * 跳过所有空白符（含换行），同时维护行/列号。
         */
        private void skipWhitespace() {
            while (pos < buf.length) {
                char c = buf[pos];
                if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                    advance();
                } else {
                    break;
                }
            }
        }

        private char currentChar() {
            return pos < buf.length ? buf[pos] : '\0';
        }

        /**
         * 指针前移并更新行/列号。
         */
        private void advance() {
            if (pos < buf.length) {
                if (buf[pos] == '\n') {
                    line++; col = 1;
                } else {
                    col++;
                }
                pos++;
            }
        }

        /**
         * 匹配下一个字符（或字符串），并前移指针。
         */
        private void expect(char c) {
            if (currentChar() != c) {
                error("期望 '" + c + "'，但遇到 '" + currentChar() + "'");
            }
            advance();
        }

        /**
         * 判断当前位置是否能完整匹配目标字符串，若能则移动指针。
         */
        private boolean match(String s) {
            int len = s.length();
            if (pos + len > buf.length) return false;
            for (int i = 0; i < len; i++) {
                if (buf[pos + i] != s.charAt(i)) return false;
            }
            for (int i = 0; i < len; i++) advance();
            return true;
        }

        private boolean isDigit(char c) {
            return c >= '0' && c <= '9';
        }

        /**
         * 抛出带行列号的解析异常。
         */
        private void error(String msg) {
            throw new UnexpectedToken(
                    "在第 " + line + " 行，第 " + col + " 列出现错误: " + msg,
                    line,
                    col
            );
        }
    }

    // ======= 内部序列化器实现 =======

    /**
     * 负责将 Java 对象序列化为 JSON 字符串。
     */
    private static class Writer {
        private static final int DEFAULT_CAPACITY = 1024;

        static String write(Object obj) {
            StringBuilder sb = new StringBuilder(DEFAULT_CAPACITY);
            writeValue(obj, sb);
            return sb.toString();
        }

        /**
         * 递归输出任意支持的 JSON 类型对象。
         */
        private static void writeValue(Object obj, StringBuilder sb) {
            if (obj == null) {
                sb.append("null");
            } else if (obj instanceof String) {
                quote((String) obj, sb);
            } else if (obj instanceof Number || obj instanceof Boolean) {
                sb.append(obj);
            } else if (obj instanceof Map) {
                sb.append('{');
                boolean first = true;
                for (Entry<?, ?> e : ((Map<?, ?>) obj).entrySet()) {
                    if (!first) sb.append(',');
                    first = false;
                    quote(e.getKey().toString(), sb);
                    sb.append(':');
                    writeValue(e.getValue(), sb);
                }
                sb.append('}');
            } else if (obj instanceof Collection) {
                sb.append('[');
                boolean first = true;
                for (Object item : (Collection<?>) obj) {
                    if (!first) sb.append(',');
                    first = false;
                    writeValue(item, sb);
                }
                sb.append(']');
            } else {
                throw new UnsupportedOperationException(
                        "不支持的 JSON 字符串化类型: " + obj.getClass());
            }
        }

        /**
         * JSON 字符串输出，处理所有必要的转义字符。
         */
        private static void quote(String s, StringBuilder sb) {
            sb.append('"');
            for (char c : s.toCharArray()) {
                switch (c) {
                    case '\\':
                        sb.append("\\\\"); break;
                    case '"':
                        sb.append("\\\""); break;
                    case '\n':
                        sb.append("\\n"); break;
                    case '\r':
                        sb.append("\\r"); break;
                    case '\t':
                        sb.append("\\t"); break;
                    default:
                        sb.append(c);
                }
            }
            sb.append('"');
        }
    }
}
