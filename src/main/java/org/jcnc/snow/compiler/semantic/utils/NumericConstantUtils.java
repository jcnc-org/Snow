package org.jcnc.snow.compiler.semantic.utils;

import org.jcnc.snow.compiler.parser.ast.BinaryExpressionNode;
import org.jcnc.snow.compiler.parser.ast.NumberLiteralNode;
import org.jcnc.snow.compiler.parser.ast.UnaryExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.semantic.type.BuiltinType;
import org.jcnc.snow.compiler.semantic.type.Type;

import java.math.BigInteger;
import java.util.Optional;

/**
 * 数值常量辅助工具。
 * <p>
 * 提供简单的“编译期整型常量”求值与范围检查，用于支持
 * byte/short 的窄化赋值规则（仅在表达式可静态求值且落在范围内时允许）。
 */
public final class NumericConstantUtils {

    private NumericConstantUtils() {
    }

    /**
     * 判断表达式是否能静态求值为整型常量，并返回其值。
     *
     * @param expr 任意表达式
     * @return 若能求值则返回常量值，否则返回 {@link Optional#empty()}
     */
    public static Optional<BigInteger> evalInteger(ExpressionNode expr) {
        if (expr == null) return Optional.empty();

        if (expr instanceof NumberLiteralNode n) {
            return parseIntegralLiteral(n.value());
        }
        if (expr instanceof UnaryExpressionNode unary) {
            Optional<BigInteger> operand = evalInteger(unary.operand());
            if (operand.isEmpty()) return Optional.empty();
            return switch (unary.operator()) {
                case "-" -> Optional.of(operand.get().negate());
                case "+" -> operand;
                default -> Optional.empty();
            };
        }
        if (expr instanceof BinaryExpressionNode bin) {
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

        return Optional.empty();
    }

    /**
     * 判断给定表达式能否作为常量安全地窄化赋值到 byte / short。
     *
     * @param target 目标类型（仅关心 byte/short）
     * @param actual 表达式推断出的类型
     * @param expr   表达式节点本身（用于常量求值）
     * @return 若允许窄化则返回 true，否则 false
     */
    public static boolean canNarrowToIntegral(Type target,
                                              Type actual,
                                              ExpressionNode expr) {
        if (!(target instanceof BuiltinType targetBuiltin)) return false;
        if (targetBuiltin != BuiltinType.BYTE && targetBuiltin != BuiltinType.SHORT) return false;
        if (!(actual instanceof BuiltinType actualBuiltin)) return false;
        // 仅允许从默认整型（int）尝试窄化
        if (actualBuiltin != BuiltinType.INT) return false;

        Optional<BigInteger> value = evalInteger(expr);
        return value.map(bigInteger -> switch (targetBuiltin) {
            case BYTE -> fitsRange(bigInteger, Byte.MIN_VALUE, Byte.MAX_VALUE);
            case SHORT -> fitsRange(bigInteger, Short.MIN_VALUE, Short.MAX_VALUE);
            default -> false;
        }).orElse(false);

    }

    // ────────── 内部工具 ──────────

    private static boolean fitsRange(BigInteger v, long min, long max) {
        return v.compareTo(BigInteger.valueOf(min)) >= 0
                && v.compareTo(BigInteger.valueOf(max)) <= 0;
    }

    private static Optional<BigInteger> parseIntegralLiteral(String raw) {
        if (raw == null) return Optional.empty();
        String digits = stripSuffix(raw.trim());
        if (digits.isEmpty() || looksLikeFloat(digits)) return Optional.empty();
        try {
            return Optional.of(new BigInteger(digits));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private static String stripSuffix(String raw) {
        if (raw.isEmpty()) return raw;
        String t = raw.replace("_", "");
        if (t.isEmpty()) return t;
        char last = Character.toLowerCase(t.charAt(t.length() - 1));
        // 去掉单个类型后缀（l/L/b/B/s/S/d/D/f/F）
        if ("lbsdf".indexOf(last) >= 0) {
            t = t.substring(0, t.length() - 1);
        }
        return t;
    }

    private static boolean looksLikeFloat(String s) {
        return s.indexOf('.') >= 0 || s.indexOf('e') >= 0 || s.indexOf('E') >= 0;
    }
}