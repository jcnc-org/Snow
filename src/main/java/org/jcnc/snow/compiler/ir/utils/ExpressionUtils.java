package org.jcnc.snow.compiler.ir.utils;

import org.jcnc.snow.compiler.ir.builder.IRContext;
import org.jcnc.snow.compiler.ir.core.IROpCode;
import org.jcnc.snow.compiler.ir.core.IROpCodeMappings;
import org.jcnc.snow.compiler.ir.value.IRConstant;
import org.jcnc.snow.compiler.parser.ast.BinaryExpressionNode;
import org.jcnc.snow.compiler.parser.ast.NumberLiteralNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;

import java.util.Map;

/**
 * 表达式分析与运算符辅助工具类。
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>字面量常量的解析与类型推断</li>
 *   <li>自动匹配算术/比较操作码</li>
 *   <li>表达式类型合并与提升</li>
 * </ul>
 */
public final class ExpressionUtils {

    private ExpressionUtils() {}

    /* ────────────────── 线程级默认类型后缀 ────────────────── */

    /** 默认类型后缀（如当前函数返回类型），线程隔离。 */
    private static final ThreadLocal<Character> DEFAULT_SUFFIX =
            ThreadLocal.withInitial(() -> '\0');

    public static void setDefaultSuffix(char suffix) { DEFAULT_SUFFIX.set(suffix); }

    public static void clearDefaultSuffix()           { DEFAULT_SUFFIX.set('\0'); }

    /* ───────────────────── 字面量 & 常量 ───────────────────── */

    /**
     * 解析整数字面量字符串，自动去除类型后缀（b/s/l/f/d/B/S/L/F/D），并转换为 int。
     */
    public static int parseIntSafely(String literal) {
        String digits = literal.replaceAll("[bslfdBSDLF]$", "");
        return Integer.parseInt(digits);
    }

    /**
     * 根据数字字面量字符串自动判断类型，生成对应类型的 {@link IRConstant}。
     * 支持 b/s/l/f/d 后缀与浮点格式。
     */
    public static IRConstant buildNumberConstant(IRContext ctx, String value) {
        char suffix = value.isEmpty() ? '\0'
                : Character.toLowerCase(value.charAt(value.length() - 1));

        String digits = switch (suffix) {
            case 'b','s','l','f','d' -> value.substring(0, value.length() - 1);
            default -> {
                /* 如果字面量本身没有后缀，则回退到变量目标类型（如声明语句左值） */
                if (ctx.getVarType() != null) {
                    String t = ctx.getVarType();
                    suffix = switch (t) {
                        case "byte"   -> 'b';
                        case "short"  -> 's';
                        case "int"    -> 'i';
                        case "long"   -> 'l';
                        case "float"  -> 'f';
                        case "double" -> 'd';
                        default       -> '\0';
                    };
                }
                yield value;
            }
        };

        /* 创建常量 */
        return switch (suffix) {
            case 'b' -> new IRConstant(Byte.parseByte(digits));
            case 's' -> new IRConstant(Short.parseShort(digits));
            case 'l' -> new IRConstant(Long.parseLong(digits));
            case 'f' -> new IRConstant(Float.parseFloat(digits));
            case 'd' -> new IRConstant(Double.parseDouble(digits));
            default  -> looksLikeFloat(digits)
                    ? new IRConstant(Double.parseDouble(digits))
                    : new IRConstant(Integer.parseInt(digits));
        };
    }

    /* ────────────────────── 一元运算 ────────────────────── */

    /**
     * 推断一元取负（-）运算应使用的 {@link IROpCode}。
     */
    public static IROpCode negOp(ExpressionNode operand) {
        char t = typeChar(operand);
        return switch (t) {
            case 'b' -> IROpCode.NEG_B8;
            case 's' -> IROpCode.NEG_S16;
            case 'l' -> IROpCode.NEG_L64;
            case 'f' -> IROpCode.NEG_F32;
            case 'd' -> IROpCode.NEG_D64;
            default  -> IROpCode.NEG_I32;      // '\0' 或 'i'
        };
    }

    /* ────────────────── 比较运算（已适配 long） ────────────────── */

    /** 判断给定字符串是否是比较运算符（==, !=, <, >, <=, >=）。 */
    public static boolean isComparisonOperator(String op) {
        return ComparisonUtils.isComparisonOperator(op);
    }

    /**
     * 兼容旧调用：仅凭操作符返回 <em>int32</em> 比较指令。
     */
    public static IROpCode cmpOp(String op) {
        return IROpCodeMappings.CMP_I32.get(op);     // 旧逻辑：一律 i32
    }

    /**
     * 推荐调用：根据左右表达式类型自动选择 int / long 比较指令。
     */
    public static IROpCode cmpOp(String op, ExpressionNode left, ExpressionNode right) {
        return ComparisonUtils.cmpOp(op, left, right);
    }

    /* ──────────────── 类型推断 & 算术操作码匹配 ──────────────── */

    /** 递归推断单个表达式节点的类型后缀（b/s/i/l/f/d）。 */
    private static char typeChar(ExpressionNode node) {
        if (node instanceof NumberLiteralNode(String value)) {
            char last = Character.toLowerCase(value.charAt(value.length() - 1));
            return switch (last) {
                case 'b','s','i','l','f','d' -> last;
                default -> looksLikeFloat(value) ? 'd' : '\0';
            };
        }
        if (node instanceof BinaryExpressionNode bin) {
            return maxTypeChar(typeChar(bin.left()), typeChar(bin.right()));
        }
        return '\0';      // 变量等暂不处理
    }

    /** 合并两侧表达式的类型后缀。 */
    public static char resolveSuffix(ExpressionNode left, ExpressionNode right) {
        return maxTypeChar(typeChar(left), typeChar(right));
    }

    /** 类型优先级：d > f > l > i > s > b > '\0' */
    private static char maxTypeChar(char l, char r) {
        if (l == 'd' || r == 'd') return 'd';
        if (l == 'f' || r == 'f') return 'f';
        if (l == 'l' || r == 'l') return 'l';
        if (l == 'i' || r == 'i') return 'i';
        if (l == 's' || r == 's') return 's';
        if (l == 'b' || r == 'b') return 'b';
        return '\0';
    }

    /**
     * 根据操作符和两侧表达式选择正确的算术 {@link IROpCode}。
     */
    public static IROpCode resolveOpCode(String op,
                                         ExpressionNode left,
                                         ExpressionNode right) {

        /* 1. 尝试根据字面量推断 */
        char suffix = resolveSuffix(left, right);

        /* 2. 若失败则使用函数级默认类型 */
        if (suffix == '\0') suffix = DEFAULT_SUFFIX.get();

        /* 3. 仍失败则默认为 int32 */
        Map<String, IROpCode> table = switch (suffix) {
            case 'b' -> IROpCodeMappings.OP_B8;
            case 's' -> IROpCodeMappings.OP_S16;
            case 'i' -> IROpCodeMappings.OP_I32;
            case 'l' -> IROpCodeMappings.OP_L64;
            case 'f' -> IROpCodeMappings.OP_F32;
            case 'd' -> IROpCodeMappings.OP_D64;
            default  -> IROpCodeMappings.OP_I32;
        };

        return table.get(op);
    }

    /* ────────────────────────── 工具 ───────────────────────── */

    /** 是否像浮点字面量（包含 '.' 或 e/E）。 */
    private static boolean looksLikeFloat(String digits) {
        return digits.indexOf('.') >= 0
                || digits.indexOf('e') >= 0
                || digits.indexOf('E') >= 0;
    }
}
