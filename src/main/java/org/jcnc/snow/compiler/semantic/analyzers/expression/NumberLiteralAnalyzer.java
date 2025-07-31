package org.jcnc.snow.compiler.semantic.analyzers.expression;

import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.parser.ast.NumberLiteralNode;
import org.jcnc.snow.compiler.semantic.analyzers.base.ExpressionAnalyzer;
import org.jcnc.snow.compiler.semantic.core.Context;
import org.jcnc.snow.compiler.semantic.core.ModuleInfo;
import org.jcnc.snow.compiler.semantic.error.SemanticError;
import org.jcnc.snow.compiler.semantic.symbol.SymbolTable;
import org.jcnc.snow.compiler.semantic.type.BuiltinType;
import org.jcnc.snow.compiler.semantic.type.Type;

import static org.jcnc.snow.compiler.semantic.type.BuiltinType.*;

/**
 * {@code NumberLiteralAnalyzer}
 *
 * <p>职责：
 * <ul>
 *   <li>对数字字面量表达式进行语义分析，并推断其精确类型；</li>
 *   <li>按照推断类型进行范围校验；</li>
 *   <li>发生越界时，给出智能的错误提示与合理建议。</li>
 * </ul>
 *
 * <p>类型推断规则：</p>
 * <ol>
 *   <li>若字面量以类型后缀(b/s/l/f，大小写均可)结尾，则按后缀直接推断目标类型：</li>
 *   <ul>
 *     <li>b → byte</li>
 *     <li>s → short</li>
 *     <li>l → long</li>
 *     <li>f → float</li>
 *   </ul>
 *   <li>若无后缀，且文本包含小数点或科学计数法('.' 或 'e/E')，则推断为 double(浮点默认 double，不支持 d/D 后缀)；</li>
 *   <li>否则推断为 int。</li>
 * </ol>
 *
 * <p>智能错误提示策略：</p>
 * <ul>
 *   <li>整数：
 *     <ul>
 *       <li>int 放不下但 long 能放下 → 直接建议 long；</li>
 *       <li>连 long 也放不下 → 一次性提示“超出 int/long 可表示范围”。</li>
 *     </ul>
 *   </li>
 *   <li>浮点：
 *     <ul>
 *       <li>float 放不下但 double 能放下 → 直接建议 double(无需 d 后缀)；</li>
 *       <li>连 double 也放不下 → 一次性提示“超出 float/double 可表示范围”。</li>
 *     </ul>
 *   </li>
 * </ul>
 */
public class NumberLiteralAnalyzer implements ExpressionAnalyzer<NumberLiteralNode> {

    /**
     * 根据字面量后缀和文本内容推断类型。
     *
     * @param hasSuffix 是否存在类型后缀(仅 b/s/l/f 有效)
     * @param suffix    后缀字符(已转小写)
     * @param digits    去掉下划线与后缀后的数字主体(可能含 . 或 e/E)
     * @return 推断类型(byte / short / int / long / float / double 之一)
     */
    private static Type inferType(boolean hasSuffix, char suffix, String digits) {
        if (hasSuffix) {
            // 仅支持 b/s/l/f；不支持 d/D(浮点默认 double)
            return switch (suffix) {
                case 'b' -> BYTE;
                case 's' -> SHORT;
                case 'l' -> BuiltinType.LONG;
                case 'f' -> BuiltinType.FLOAT;
                default -> INT; // 其他后缀当作无效，按 int 处理(如需严格，可改为抛/报“非法后缀”)
            };
        }
        // 无后缀：包含小数点或 e/E → double；否则 int
        if (looksLikeFloat(digits)) {
            return BuiltinType.DOUBLE; // 浮点默认 double
        }
        return INT;
    }

    /**
     * 做范围校验，发生越界时写入智能的错误与建议。
     *
     * @param ctx      语义上下文(承载错误列表与日志)
     * @param node     当前数字字面量节点
     * @param inferred 推断类型
     * @param digits   规整后的数字主体(去下划线、去后缀、"123."→"123.0")
     */
    private static void validateRange(Context ctx,
                                      NumberLiteralNode node,
                                      Type inferred,
                                      String digits) {
        try {
            // —— 整数类型：不允许出现浮点形式 —— //
            if (inferred == BYTE) {
                if (looksLikeFloat(digits)) throw new NumberFormatException(digits);
                Byte.parseByte(digits);
            } else if (inferred == SHORT) {
                if (looksLikeFloat(digits)) throw new NumberFormatException(digits);
                Short.parseShort(digits);
            } else if (inferred == INT) {
                if (looksLikeFloat(digits)) throw new NumberFormatException(digits);
                Integer.parseInt(digits);
            } else if (inferred == BuiltinType.LONG) {
                if (looksLikeFloat(digits)) throw new NumberFormatException(digits);
                Long.parseLong(digits);
            }
            // —— 浮点类型：解析 + 上/下溢判断 —— //
            else if (inferred == BuiltinType.FLOAT) {
                float v = Float.parseFloat(digits);
                // 上溢：Infinity；下溢：解析为 0.0 但文本并非“全零”
                if (Float.isInfinite(v) || (v == 0.0f && isTextualZero(digits))) {
                    throw new NumberFormatException("float overflow/underflow: " + digits);
                }
            } else if (inferred == BuiltinType.DOUBLE) {
                double v = Double.parseDouble(digits);
                if (Double.isInfinite(v) || (v == 0.0 && isTextualZero(digits))) {
                    throw new NumberFormatException("double overflow/underflow: " + digits);
                }
            }
        } catch (NumberFormatException ex) {
            // 智能的错误描述与建议(header 使用 digits，避免带后缀)
            String msg = getSmartSuggestionOneShot(digits, inferred);
            ctx.getErrors().add(new SemanticError(node, msg));
            ctx.log("错误: " + msg);
        }
    }

    /**
     * 生成智能的错误提示与建议。
     *
     * <p>策略：</p>
     * <ul>
     *   <li>BYTE/SHORT/INT：若能放进更大整型，直接建议；否则一次性提示已超出 int/long 范围；</li>
     *   <li>LONG：直接提示“超出 long 可表示范围。”；</li>
     *   <li>FLOAT：若 double 能放下 → 建议 double；否则一次性提示“超出 float/double 可表示范围。”；</li>
     *   <li>DOUBLE：直接提示“超出 double 可表示范围。”。</li>
     * </ul>
     *
     * @param digits   去后缀后的数字主体(用于 header 与建议示例)
     * @param inferred 推断类型
     * @return 完整错误消息(含建议)
     */
    private static String getSmartSuggestionOneShot(String digits, Type inferred) {
        String header;
        String suggestion;

        switch (inferred) {
            case BYTE -> {
                long v;
                try {
                    v = Long.parseLong(digits);
                } catch (NumberFormatException e) {
                    // 连 long 都放不下：智能
                    header = composeHeader(digits, "超出 byte/short/int/long 可表示范围。");
                    return header;
                }
                if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) {
                    header = composeHeader(digits, "超出 byte 可表示范围。");
                    suggestion = "建议将变量类型声明为 short，并在数字末尾添加 's'(如 " + digits + "s)。";
                } else if (v >= Integer.MIN_VALUE && v <= Integer.MAX_VALUE) {
                    header = composeHeader(digits, "超出 byte 可表示范围。");
                    suggestion = "建议将变量类型声明为 int(如 " + digits + ")。";
                } else {
                    header = composeHeader(digits, "超出 byte 可表示范围。");
                    suggestion = "建议将变量类型声明为 long，并在数字末尾添加 'L'(如 " + digits + "L)。";
                }
                return appendSuggestion(header, suggestion);
            }
            case SHORT -> {
                long v;
                try {
                    v = Long.parseLong(digits);
                } catch (NumberFormatException e) {
                    header = composeHeader(digits, "超出 short/int/long 可表示范围。");
                    return header;
                }
                if (v >= Integer.MIN_VALUE && v <= Integer.MAX_VALUE) {
                    header = composeHeader(digits, "超出 short 可表示范围。");
                    suggestion = "建议将变量类型声明为 int(如 " + digits + ")。";
                } else {
                    header = composeHeader(digits, "超出 short 可表示范围。");
                    suggestion = "建议将变量类型声明为 long，并在数字末尾添加 'L'(如 " + digits + "L)。";
                }
                return appendSuggestion(header, suggestion);
            }
            case INT -> {
                try {
                    // 尝试解析为 long：若成功，说明“能进 long”
                    Long.parseLong(digits);
                } catch (NumberFormatException e) {
                    // 连 long 都放不下：智能
                    header = composeHeader(digits, "超出 int/long 可表示范围。");
                    return header;
                }
                // 能进 long：直接建议 long
                header = composeHeader(digits, "超出 int 可表示范围。");
                suggestion = "建议将变量类型声明为 long，并在数字末尾添加 'L'(如 " + digits + "L)。";
                return appendSuggestion(header, suggestion);
            }
            case LONG -> {
                // 已明确处于 long 分支且越界：智能
                header = composeHeader(digits, "超出 long 可表示范围。");
                return header;
            }
            case FLOAT -> {
                // float 放不下：尝试 double，若能放下则直接建议 double；否则智能提示 float/double 都不行
                boolean fitsDouble = fitsDouble(digits);
                if (fitsDouble) {
                    header = composeHeader(digits, "超出 float 可表示范围。");
                    suggestion = "建议将变量类型声明为 double(如 " + digits + ")。"; // double 默认，无需 d 后缀
                    return appendSuggestion(header, suggestion);
                } else {
                    header = composeHeader(digits, "超出 float/double 可表示范围。");
                    return header;
                }
            }
            case DOUBLE -> {
                header = composeHeader(digits, "超出 double 可表示范围。");
                return header;
            }
            default -> {
                header = composeHeader(digits, "超出 " + inferred + " 可表示范围。");
                return header;
            }
        }
    }

    /**
     * 生成越界错误头部：统一使用“数字主体”而非原始 raw(避免带后缀引起误导)。
     *
     * @param digits 去后缀后的数字主体
     * @param tail   错误尾部描述(如“超出 int 可表示范围。”)
     */
    private static String composeHeader(String digits, String tail) {
        return "数值字面量越界: \"" + digits + "\" " + tail;
    }

    /**
     * 在头部后拼接建议文本(若存在)。
     *
     * @param header     错误头部
     * @param suggestion 建议文本(可能为 null)
     */
    private static String appendSuggestion(String header, String suggestion) {
        return suggestion == null ? header : header + " " + suggestion;
    }

    /**
     * 文本层面判断“是否看起来是浮点字面量”：
     * 只要包含 '.' 或 'e/E'，即视为浮点。
     */
    private static boolean looksLikeFloat(String s) {
        return s.indexOf('.') >= 0 || s.indexOf('e') >= 0 || s.indexOf('E') >= 0;
    }

    /**
     * 文本判断“是否为零”(不解析，纯文本)：
     * 在遇到 e/E 之前，若出现任意 '1'..'9'，视为非零；否则视为零。
     * 用于识别“文本非零但解析结果为 0.0”的下溢场景。
     * <p>
     * 例：
     * "0.0"         → true
     * "000"         → true
     * "1e-9999"     → false(e 前有 '1'；若解析为 0.0 则视为下溢)
     * "0e-9999"     → true
     */
    private static boolean isTextualZero(String s) {
        if (s == null || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == 'e' || c == 'E') break;   // 指数部分不参与“是否为零”的判断
            if (c >= '1' && c <= '9') return true;
        }
        return false;
    }

    /**
     * 判断 double 是否能正常表示(不溢出、也非“文本非零但解析为 0.0”的下溢)。
     *
     * @param digits 去后缀后的数字主体
     */
    private static boolean fitsDouble(String digits) {
        try {
            double d = Double.parseDouble(digits);
            return !Double.isInfinite(d) && !(d == 0.0 && isTextualZero(digits));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 规整数字串：
     * 仅移除末尾的类型后缀(仅 b/s/l/f，大小写均可，不含 d/D)。
     *
     * @param s 原始字面量字符串
     * @return 规整后的数字主体
     */
    private static String normalizeDigits(String s) {
        if (s == null) return "";
        String t = s.trim();
        if (t.isEmpty()) return t;

        // 仅移除末尾的类型后缀(b/s/l/f，大小写均可)
        char last = t.charAt(t.length() - 1);
        if ("bBsSfFlL".indexOf(last) >= 0) {
            t = t.substring(0, t.length() - 1).trim();
        }
        return t;
    }


    /**
     * 入口：对数字字面量进行语义分析。
     * <p>
     * 分步：
     * <ol>
     *   <li>读取原始文本 raw；</li>
     *   <li>识别是否带后缀(仅 b/s/l/f)；</li>
     *   <li>规整数字主体 digits(去下划线、去后缀、补小数点零)；</li>
     *   <li>按规则推断目标类型；</li>
     *   <li>做范围校验，越界时记录智能的错误与建议；</li>
     *   <li>返回推断类型。</li>
     * </ol>
     */
    @Override
    public Type analyze(Context ctx,
                        ModuleInfo mi,
                        FunctionNode fn,
                        SymbolTable locals,
                        NumberLiteralNode expr) {

        // 1) 原始文本(如 "123", "3.14", "2f", "1_000_000L", "1e300")
        String raw = expr.value();
        if (raw == null || raw.isEmpty()) {
            return INT; // 空文本回退为 int(按需可改为错误)
        }

        // 2) 是否带后缀(仅 b/s/l/f；不支持 d/D)
        char lastChar = raw.charAt(raw.length() - 1);
        char suffix = Character.toLowerCase(lastChar);
        boolean hasSuffix = switch (suffix) {
            case 'b', 's', 'l', 'f' -> true;
            default -> false;
        };

        // 3) 规整数字主体
        String digitsNormalized = normalizeDigits(raw);

        // 4) 推断类型
        Type inferred = inferType(hasSuffix, suffix, digitsNormalized);

        // 5) 范围校验(发生越界则收集智能的错误与建议)
        validateRange(ctx, expr, inferred, digitsNormalized);

        return inferred;
    }

}
