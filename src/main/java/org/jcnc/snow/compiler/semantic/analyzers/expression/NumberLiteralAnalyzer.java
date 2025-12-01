package org.jcnc.snow.compiler.semantic.analyzers.expression;

import org.jcnc.snow.compiler.common.NumberLiteralHelper;
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
 * 数字字面量的语义分析器 {@code NumberLiteralAnalyzer}。
 *
 * <p>主要职责：</p>
 * <ul>
 *     <li>对数字字面量进行语义分析并推断其精确类型；</li>
 *     <li>根据目标类型执行数值范围校验；</li>
 *     <li>在越界或格式错误时提供智能、友好的错误提示与建议。</li>
 * </ul>
 *
 * <p><b>类型推断规则：</b></p>
 * <ol>
 *     <li>仅保留 <code>l/L</code>（long）与 <code>f/F</code>（float）作为显式类型后缀；不再支持 <code>b/s/d</code>；</li>
 *     <li>无后缀且文本中包含小数点或 e/E → 推断为 <code>double</code>；</li>
 *     <li>其他无后缀的整数文本 → 推断为 <code>int</code>；若值超出 int 范围则要求显式写 L。</li>
 * </ol>
 *
 * <p><b>智能错误提示策略：</b></p>
 * <ul>
 *     <li><b>整数类：</b>
 *         <ul>
 *             <li>若 int 越界但 long 可容纳 → 自动建议使用 long（添加 <code>L</code> 后缀）；</li>
 *             <li>若 int 和 long 都无法容纳 → 一次性提示“超出 int/long 可表示范围”。</li>
 *         </ul>
 *     </li>
 *     <li><b>浮点类：</b>
 *         <ul>
 *             <li>若 float 越界但 double 可容纳 → 自动建议使用 double（默认即可，无需 <code>d</code> 后缀）；</li>
 *             <li>若 double 也无法容纳 → 一次性提示“超出 float/double 可表示范围”。</li>
 *         </ul>
 *     </li>
 * </ul>
 */
public class NumberLiteralAnalyzer implements ExpressionAnalyzer<NumberLiteralNode> {

    /**
     * 根据字面量后缀及数字文本推断其目标类型。
     *
     * @param hasSuffix 是否存在受支持的类型后缀（仅 l、f）
     * @param suffix    后缀字符（小写形式，仅在 hasSuffix=true 时有效）
     * @param digits    去除下划线和类型后缀后的文本（可能包含小数点/科学计数法）
     * @param radix     进制（10 或 16）
     * @return 推断得出的类型（int/long/float/double）
     */
    private static Type inferType(boolean hasSuffix, char suffix, String digits, int radix) {
        if (radix == 16) {
            // 十六进制：默认 int；显式 L 则 long
            return hasSuffix && suffix == 'l' ? BuiltinType.LONG : INT;
        }

        if (hasSuffix) {
            // 仅 l/f 有效；d 不支持（double 默认不需后缀）
            return switch (suffix) {
                case 'l' -> BuiltinType.LONG;
                case 'f' -> BuiltinType.FLOAT;
                default -> INT; // 其他后缀视为无效，退回 int
            };
        }

        // 无后缀：若含小数点/e/E → double，否则 int
        if (looksLikeFloat(digits)) {
            return BuiltinType.DOUBLE;
        }

        return INT;
    }

    /**
     * 执行范围校验，若超出对应类型可表示范围，则写入带智能建议的错误消息。
     *
     * @param ctx        语义上下文（包含错误列表）
     * @param node       字面量节点
     * @param inferred   推断出的目标类型
     * @param normalized 规整后的字面量（包含 digits/text/radix）
     */
    private static void validateRange(Context ctx,
                                      NumberLiteralNode node,
                                      Type inferred,
                                      NumberLiteralHelper.NormalizedLiteral normalized) {
        String digits = normalized.text();
        String payload = normalized.digits();
        int radix = normalized.radix();

        try {
            // ---------------------- 整数类型范围校验 ----------------------
            if (inferred == INT) {
                if (looksLikeFloat(digits)) throw new NumberFormatException(digits);
                NumberLiteralHelper.parseIntLiteral(payload, radix);
            } else if (inferred == BuiltinType.LONG) {
                if (looksLikeFloat(digits)) throw new NumberFormatException(digits);
                NumberLiteralHelper.parseLongLiteral(payload, radix);
            }

            // ---------------------- 浮点类型范围校验 ----------------------
            else if (inferred == BuiltinType.FLOAT) {
                float v = Float.parseFloat(digits);
                if (Float.isInfinite(v) || (v == 0.0f && isTextualNonZero(digits))) {
                    throw new NumberFormatException("float overflow/underflow");
                }
            } else if (inferred == BuiltinType.DOUBLE) {
                double v = Double.parseDouble(digits);
                if (Double.isInfinite(v) || (v == 0.0 && isTextualNonZero(digits))) {
                    throw new NumberFormatException("double overflow/underflow");
                }
            }

        } catch (NumberFormatException ex) {
            // 越界：生成智能建议消息
            String msg = getSmartSuggestionOneShot(normalized, inferred);
            ctx.getErrors().add(new SemanticError(node, msg));
            ctx.log("错误: " + msg);
        }
    }

    /**
     * 根据数值文本与推断类型生成一次性智能提示。
     *
     * @param normalized 规整后的字面量信息
     * @param inferred   推断类型
     * @return 错误文本（可能带改进建议）
     */
    private static String getSmartSuggestionOneShot(NumberLiteralHelper.NormalizedLiteral normalized, Type inferred) {
        String digits = normalized.text();
        String payload = normalized.digits();
        int radix = normalized.radix();

        switch (inferred) {
            case INT -> {
                // 若 int 装不下但 long 能装下 → 建议加 L 后缀
                boolean fitsLong;
                try {
                    NumberLiteralHelper.parseLongLiteral(payload, radix);
                    fitsLong = true;
                } catch (NumberFormatException ignored) {
                    fitsLong = false;
                }

                if (fitsLong) {
                    return composeHeader(digits, "超出 int 可表示范围。")
                            + " 建议在数字末尾添加 'L' 或将变量声明为 long。例如：" + digits + "L。";
                }

                return composeHeader(digits, "超出 int/long 可表示范围。");
            }

            case LONG -> {
                return composeHeader(digits, "超出 long 可表示范围。");
            }

            case FLOAT -> {
                if (fitsDouble(digits)) {
                    return composeHeader(digits, "超出 float 可表示范围。")
                            + " 建议使用 double 类型（无需 d 后缀）。";
                }
                return composeHeader(digits, "超出 float/double 可表示范围。");
            }

            case DOUBLE -> {
                return composeHeader(digits, "超出 double 可表示范围。");
            }

            default -> {
                return composeHeader(digits, "超出 " + inferred + " 可表示范围。");
            }
        }
    }

    /**
     * 构造错误信息头部。
     *
     * @param digits 数值主体
     * @param tail   错误描述
     */
    private static String composeHeader(String digits, String tail) {
        return "数值字面量越界: \"" + digits + "\" " + tail;
    }

    /**
     * 浮点文本判断：只要含 '.' 或 e/E 即判为浮点。
     */
    private static boolean looksLikeFloat(String s) {
        return NumberLiteralHelper.looksLikeFloat(s);
    }

    /**
     * 文本是否包含“非零数字”。用于判断 float/double 下溢：
     * <ul>
     *     <li>若文本包含非零数字但解析结果为 0.0 → 下溢</li>
     * </ul>
     */
    private static boolean isTextualNonZero(String s) {
        if (s == null || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == 'e' || c == 'E') break; // 指数部分不参与“是否为零”
            if (c >= '1' && c <= '9') return true;
        }
        return false;
    }

    /**
     * 判断 double 是否能够正常表示给定的文本。
     */
    private static boolean fitsDouble(String digits) {
        try {
            double d = Double.parseDouble(digits);
            return !Double.isInfinite(d) && !(d == 0.0 && isTextualNonZero(digits));
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    /**
     * 数字字面量语义分析入口。
     *
     * <p>分析步骤：</p>
     * <ol>
     *     <li>解析原始文本；</li>
     *     <li>识别并校验类型后缀（仅支持 l/f）；</li>
     *     <li>规整数字主体（去下划线、去后缀）；</li>
     *     <li>根据文本与后缀推断类型；</li>
     *     <li>按类型执行范围校验；</li>
     *     <li>返回推断类型。</li>
     * </ol>
     */
    @Override
    public Type analyze(Context ctx,
                        ModuleInfo mi,
                        FunctionNode fn,
                        SymbolTable locals,
                        NumberLiteralNode expr) {

        String raw = expr.value();
        if (raw == null || raw.isEmpty()) {
            return INT; // 空文本默认 int
        }

        // 后缀识别（仅 l/f 有效）
        char suffix = NumberLiteralHelper.extractTypeSuffix(raw);
        boolean hasSuffix = suffix == 'l' || suffix == 'f';
        boolean legacySuffix = suffix == 'b' || suffix == 's';
        boolean unsupportedSuffix = legacySuffix || suffix == 'd';

        // 不支持的后缀给出错误提示
        if (unsupportedSuffix) {
            char lastChar = raw.charAt(raw.length() - 1);
            String msg = legacySuffix
                    ? "byte/short 字面量不再支持通过后缀声明，请去掉尾部 '" + lastChar + "'。"
                    : "不支持的数字字面量后缀 '" + lastChar + "'（浮点默认 double）。";
            ctx.getErrors().add(new SemanticError(expr, msg));
            ctx.log("错误: " + msg);
        }

        // 规整字面量
        NumberLiteralHelper.NormalizedLiteral normalized =
                NumberLiteralHelper.normalize(raw, hasSuffix || unsupportedSuffix);
        String digitsNormalized = normalized.text();

        // 类型推断
        Type inferred = inferType(hasSuffix, suffix, digitsNormalized, normalized.radix());

        // 范围校验
        validateRange(ctx, expr, inferred, normalized);

        return inferred;
    }
}