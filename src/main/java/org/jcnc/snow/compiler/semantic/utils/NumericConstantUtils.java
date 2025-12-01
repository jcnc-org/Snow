package org.jcnc.snow.compiler.semantic.utils;

import org.jcnc.snow.compiler.common.NumberLiteralHelper;
import org.jcnc.snow.compiler.parser.ast.BinaryExpressionNode;
import org.jcnc.snow.compiler.parser.ast.NumberLiteralNode;
import org.jcnc.snow.compiler.parser.ast.UnaryExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.semantic.type.BuiltinType;
import org.jcnc.snow.compiler.semantic.type.Type;

import java.math.BigInteger;
import java.util.Optional;

/**
 * 数值常量辅助工具类。
 *
 * <p>
 * 提供简单的“编译期整型常量求值”能力，用于支持以下语义特性：
 * </p>
 * <ul>
 *     <li>编译期对数字表达式进行静态求值；</li>
 *     <li>用于 byte / short 的窄化赋值规则（仅当表达式可静态求值且数值落在目标范围内时允许）；</li>
 *     <li>避免 long / float / double 在无提示情况下发生静默截断。</li>
 * </ul>
 *
 * <p>
 * 该工具类仅处理整型常量求值，不涉及浮点常量求值。
 * </p>
 */
public final class NumericConstantUtils {

    private NumericConstantUtils() {
    }

    /**
     * 尝试对表达式进行编译期整型求值。
     *
     * <p>
     * 支持的表达式形式：
     * </p>
     * <ul>
     *     <li>纯数字字面量；</li>
     *     <li>一元符号表达式：{@code +x}、{@code -x}；</li>
     *     <li>二元整数运算：{@code + - * / % << >>}；</li>
     * </ul>
     *
     * <p>
     * 若表达式可全部在编译期求值，则返回常量 {@link BigInteger}；否则返回 {@link Optional#empty()}。
     * </p>
     *
     * @param expr 任意表达式节点
     * @return 整型常量值（若不可求值则返回 empty）
     */
    public static Optional<BigInteger> evalInteger(ExpressionNode expr) {
        switch (expr) {
            case null -> {
                return Optional.empty();
            }

            // 数字字面量
            case NumberLiteralNode n -> {
                return parseIntegralLiteral(n.value());
            }

            // 一元运算：+x / -x
            case UnaryExpressionNode unary -> {
                Optional<BigInteger> operand = evalInteger(unary.operand());
                if (operand.isEmpty()) return Optional.empty();
                return switch (unary.operator()) {
                    case "-" -> Optional.of(operand.get().negate());
                    case "+" -> operand;
                    default -> Optional.empty();
                };
            }

            // 二元运算：+ - * / % << >>
            case BinaryExpressionNode bin -> {
                Optional<BigInteger> l = evalInteger(bin.left());
                Optional<BigInteger> r = evalInteger(bin.right());
                if (l.isEmpty() || r.isEmpty()) return Optional.empty();
                BigInteger a = l.get();
                BigInteger b = r.get();
                return switch (bin.operator()) {
                    case "+" -> Optional.of(a.add(b));
                    case "-" -> Optional.of(a.subtract(b));
                    case "*" -> Optional.of(a.multiply(b));
                    case "/" -> b.equals(BigInteger.ZERO) ? Optional.empty() : Optional.of(a.divide(b));
                    case "%" -> b.equals(BigInteger.ZERO) ? Optional.empty() : Optional.of(a.remainder(b));
                    case "<<" -> Optional.of(a.shiftLeft(b.intValue()));
                    case ">>" -> Optional.of(a.shiftRight(b.intValue()));
                    default -> Optional.empty();
                };
            }
            default -> {
            }
        }

        return Optional.empty();
    }

    /**
     * 判断表达式能否作为常量安全地窄化赋值到 byte 或 short。
     *
     * <p>
     * 仅当满足以下条件时返回 true：
     * </p>
     * <ul>
     *     <li>目标类型是 byte 或 short；</li>
     *     <li>源类型为 int（符合 Java/Snow 窄化规则的前置条件）；</li>
     *     <li>表达式可静态求值；</li>
     *     <li>求值结果落在 byte/short 的合法范围内。</li>
     * </ul>
     *
     * @param target 目标类型（byte 或 short）
     * @param actual 源表达式推断类型
     * @param expr   表达式节点
     * @return 是否允许窄化赋值
     */
    public static boolean canNarrowToIntegral(Type target,
                                              Type actual,
                                              ExpressionNode expr) {
        if (!(target instanceof BuiltinType targetBuiltin)) return false;
        if (targetBuiltin != BuiltinType.BYTE && targetBuiltin != BuiltinType.SHORT) return false;
        if (!(actual instanceof BuiltinType actualBuiltin)) return false;

        // 仅允许从 int 尝试窄化
        if (actualBuiltin != BuiltinType.INT) return false;

        Optional<BigInteger> value = evalInteger(expr);

        return value.map(bigInteger -> switch (targetBuiltin) {
            case BYTE -> fitsRange(bigInteger, Byte.MIN_VALUE, Byte.MAX_VALUE);
            case SHORT -> fitsRange(bigInteger, Short.MIN_VALUE, Short.MAX_VALUE);
            default -> false;
        }).orElse(false);
    }

    /**
     * 运行期允许的整型窄化规则：
     *
     * <p>
     * 目标为 byte/short，且源类型不宽于 int（允许来自 byte, short, int）。
     * </p>
     *
     * <p>
     * 用于允许像 {@code parseShort(...)} 这样的运行期表达式赋值，同时阻止 long/float/double 的无提示截断。
     * </p>
     *
     * @param target 目标类型
     * @param actual 源类型
     * @return 是否允许运行期整型窄化
     */
    public static boolean allowIntegralNarrowing(Type target, Type actual) {
        if (!(target instanceof BuiltinType t)) return false;
        if (t != BuiltinType.BYTE && t != BuiltinType.SHORT) return false;
        if (!(actual instanceof BuiltinType a)) return false;

        return a == BuiltinType.INT || a == BuiltinType.SHORT || a == BuiltinType.BYTE;
    }

    private static boolean fitsRange(BigInteger v, long min, long max) {
        return v.compareTo(BigInteger.valueOf(min)) >= 0
                && v.compareTo(BigInteger.valueOf(max)) <= 0;
    }

    /**
     * 尝试解析整数字面量并返回其 BigInteger 值。
     *
     * <p>
     * 若字面量格式非法、包含小数点或科学计数法，则返回 empty。
     * </p>
     */
    private static Optional<BigInteger> parseIntegralLiteral(String raw) {
        if (raw == null) return Optional.empty();

        var normalized = NumberLiteralHelper.normalize(raw.trim(), true);
        String digits = normalized.digits();

        if (digits.isEmpty() || NumberLiteralHelper.looksLikeFloat(normalized.text())) {
            return Optional.empty();
        }

        try {
            return Optional.of(new BigInteger(digits, normalized.radix()));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}