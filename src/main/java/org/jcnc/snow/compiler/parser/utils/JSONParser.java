package org.jcnc.snow.compiler.parser.utils;

import org.jcnc.snow.compiler.parser.context.UnexpectedToken;

import java.util.*;
import java.util.Map.Entry;

/**
 * JSON 工具类，提供线程安全、可重用的解析与序列化功能
 * <p>
 * - 解析：将合法的 JSON 文本转换为 Java 原生对象（Map、List、String、Number、Boolean 或 null）
 * - 序列化：将 Java 原生对象转换为符合 JSON 标准的字符串
 * <p>
 * 设计要点：
 * 1. 使用静态方法作为唯一入口，避免状态共享导致的线程安全问题
 * 2. 解析器内部使用 char[] 缓冲区，提高访问性能
 * 3. 维护行列号信息，抛出异常时能精确定位错误位置
 * 4. 序列化器基于 StringBuilder，预分配容量，减少中间字符串创建
 */
public class JSONParser {

    private JSONParser() {
    }

    /**
     * 将 JSON 文本解析为对应的 Java 对象
     *
     * @param input JSON 格式字符串
     * @return 对应的 Java 原生对象：
     * - JSON 对象 -> Map<String, Object>
     * - JSON 数组 -> List<Object>
     * - JSON 字符串 -> String
     * - JSON 数值 -> Long 或 Double
     * - JSON 布尔 -> Boolean
     * - JSON null -> null
     * @throws UnexpectedToken 如果遇到语法错误或多余字符，异常消息中包含行列信息
     */
    public static Object parse(String input) {
        return new Parser(input).parseInternal();
    }

    /**
     * 将 Java 原生对象序列化为 JSON 字符串
     *
     * @param obj 支持的类型：Map、Collection、String、Number、Boolean 或 null
     * @return 符合 JSON 规范的字符串
     */
    public static String toJson(Object obj) {
        return Writer.write(obj);
    }

    // ======= 内部解析器 =======

    /**
     * 负责将 char[] 缓冲区中的 JSON 文本解析为 Java 对象
     */
    private static class Parser {
        /**
         * 输入缓冲区
         */
        private final char[] buf;
        /**
         * 当前解析到的位置索引
         */
        private int pos;
        /**
         * 当前字符所在行号，从 1 开始
         */
        private int line;
        /**
         * 当前字符所在列号，从 1 开始
         */
        private int col;

        /**
         * 构造解析器，初始化缓冲区和行列信息
         *
         * @param input 待解析的 JSON 文本
         */
        Parser(String input) {
            this.buf = input.toCharArray();
            this.pos = 0;
            this.line = 1;
            this.col = 1;
        }

        /**
         * 入口方法，跳过空白后调用 parseValue，再校验尾部无多余字符
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
         * 根据下一个字符决定解析哪种 JSON 值
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
            return null; // 永不到达
        }

        /**
         * 解析 JSON 对象，返回 Map<String, Object>
         */
        private Map<String, Object> parseObject() {
            expect('{'); // 跳过 '{'
            skipWhitespace();
            Map<String, Object> map = new LinkedHashMap<>();
            // 空对象 {}
            if (currentChar() == '}') {
                advance(); // 跳过 '}'
                return map;
            }
            // 多成员对象解析
            while (true) {
                skipWhitespace();
                String key = parseString(); // 解析键
                skipWhitespace();
                expect(':');
                skipWhitespace();
                Object val = parseValue(); // 解析值
                map.put(key, val);
                skipWhitespace();
                if (currentChar() == '}') {
                    advance(); // 跳过 '}'
                    break;
                }
                expect(',');
                skipWhitespace();
            }
            return map;
        }

        /**
         * 解析 JSON 数组，返回 List<Object>
         */
        private List<Object> parseArray() {
            expect('[');
            skipWhitespace();
            List<Object> list = new ArrayList<>();
            // 空数组 []
            if (currentChar() == ']') {
                advance(); // 跳过 ']'
                return list;
            }
            // 多元素数组解析
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
         * 解析 JSON 字符串文字，处理转义字符
         */
        private String parseString() {
            expect('"'); // 跳过开头 '"'
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = currentChar();
                if (c == '"') {
                    advance(); // 跳过结束 '"'
                    break;
                }
                if (c == '\\') {
                    advance(); // 跳过 '\'
                    c = currentChar();
                    switch (c) {
                        case '"':
                            sb.append('"');
                            break;
                        case '\\':
                            sb.append('\\');
                            break;
                        case '/':
                            sb.append('/');
                            break;
                        case 'b':
                            sb.append('\b');
                            break;
                        case 'f':
                            sb.append('\f');
                            break;
                        case 'n':
                            sb.append('\n');
                            break;
                        case 'r':
                            sb.append('\r');
                            break;
                        case 't':
                            sb.append('\t');
                            break;
                        case 'u': // 解析 Unicode 转义
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
         * 解析 JSON 数值，支持整数、浮点及科学计数法
         */
        private Number parseNumber() {
            int start = pos;
            if (currentChar() == '-') advance();
            while (isDigit(currentChar())) advance();
            if (currentChar() == '.') {
                do advance();
                while (isDigit(currentChar()));
            }
            if (currentChar() == 'e' || currentChar() == 'E') {
                advance();
                if (currentChar() == '+' || currentChar() == '-') advance();
                while (isDigit(currentChar())) advance();
            }
            String num = new String(buf, start, pos - start);
            // 判断返回 Long 还是 Double
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
         * 跳过所有空白字符，支持空格、制表符、回车、换行
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

        /**
         * 获取当前位置字符，超出范围返回 '\0'
         */
        private char currentChar() {
            return pos < buf.length ? buf[pos] : '\0';
        }

        /**
         * 推进到下一个字符，并更新行列信息
         */
        private void advance() {
            if (pos < buf.length) {
                if (buf[pos] == '\n') {
                    line++;
                    col = 1;
                } else {
                    col++;
                }
                pos++;
            }
        }

        /**
         * 验证当前位置字符等于预期字符，否则抛出错误
         */
        private void expect(char c) {
            if (currentChar() != c) {
                error("期望 '" + c + "'，但遇到 '" + currentChar() + "'");
            }
            advance();
        }

        /**
         * 尝试匹配给定字符串，匹配成功则移动位置并返回 true
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

        /**
         * 判断字符是否为数字
         */
        private boolean isDigit(char c) {
            return c >= '0' && c <= '9';
        }

        /**
         * 抛出带行列定位的解析错误
         */
        private void error(String msg) {
            throw new UnexpectedToken("在第 " + line + " 行，第 " + col + " 列出现错误: " + msg);
        }
    }

    // ======= 内部序列化器 =======

    /**
     * 负责高效地将 Java 对象写为 JSON 文本
     */
    private static class Writer {
        /**
         * 默认 StringBuilder 初始容量，避免频繁扩容
         */
        private static final int DEFAULT_CAPACITY = 1024;

        /**
         * 入口方法，根据 obj 类型分派写入逻辑
         */
        static String write(Object obj) {
            StringBuilder sb = new StringBuilder(DEFAULT_CAPACITY);
            writeValue(obj, sb);
            return sb.toString();
        }

        /**
         * 根据对象类型选择合适的写入方式
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
         * 为字符串添加双引号并转义必须的字符
         */
        private static void quote(String s, StringBuilder sb) {
            sb.append('"');
            for (char c : s.toCharArray()) {
                switch (c) {
                    case '\\':
                        sb.append("\\\\");
                        break;
                    case '"':
                        sb.append("\\\"");
                        break;
                    case '\n':
                        sb.append("\\n");
                        break;
                    case '\r':
                        sb.append("\\r");
                        break;
                    case '\t':
                        sb.append("\\t");
                        break;
                    default:
                        sb.append(c);
                }
            }
            sb.append('"');
        }
    }
}
