package org.jcnc.snow.compiler.ir.utils;

import org.jcnc.snow.compiler.ir.builder.core.IRContext;
import org.jcnc.snow.compiler.ir.core.IROpCode;
import org.jcnc.snow.compiler.ir.core.IROpCodeMappings;
import org.jcnc.snow.compiler.ir.value.IRConstant;
import org.jcnc.snow.compiler.parser.ast.BinaryExpressionNode;
import org.jcnc.snow.compiler.parser.ast.NumberLiteralNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;

import java.util.Map;

/**
 * 表达式分析与操作符选择工具类。
 * <p>
 * 主要功能:
 * - 解析字面量常量，自动推断类型
 * - 自动匹配并选择适合的算术/比较操作码
 * - 表达式类型的合并与类型提升
 * - 支持线程隔离的函数级默认类型后缀
 */
public final class ExpressionUtils {

    /**
     * 当前线程的默认类型后缀（如当前函数返回类型等），用于类型推断兜底。
     */
    private static final ThreadLocal<Character> DEFAULT_SUFFIX =
            ThreadLocal.withInitial(() -> '\0');

    // ───────────── 线程级默认类型后缀 ─────────────

    private ExpressionUtils() {
    }

    /**
     * 设置当前线程的默认类型后缀。
     *
     * @param suffix 类型后缀字符（b/s/i/l/f/d），'\0'表示无
     */
    public static void setDefaultSuffix(char suffix) {
        DEFAULT_SUFFIX.set(suffix);
    }

    /**
     * 清除当前线程的默认类型后缀，重置为无。
     */
    public static void clearDefaultSuffix() {
        DEFAULT_SUFFIX.set('\0');
    }

    // ───────────── 字面量常量解析 ─────────────

    /**
     * 安全解析整数字面量字符串，自动去除单字符类型后缀（b/s/l/f/d，大小写均可），并转换为 int。
     *
     * @param literal 字面量字符串
     * @return 字面量对应的 int 数值
     * @throws NumberFormatException 如果字面量无法转换为整数
     */
    public static int parseIntSafely(String literal) {
        String digits = literal.replaceAll("[bslfdBSDLF]$", "");
        return Integer.parseInt(digits);
    }

    /**
     * 根据数字字面量字符串推断类型并生成对应的 IRConstant 常量值。
     * <p>
     * 支持的字面量后缀有 b/s/l/f/d（大小写均可）。
     * 无后缀时，优先参考 IRContext 当前变量类型，否则根据字面量格式（含'.'或'e'等）判断为 double，否则为 int。
     *
     * @param ctx   IRContext，允许参考变量声明类型
     * @param value 数字字面量字符串
     * @return 对应类型的 IRConstant 常量
     */
    public static IRConstant buildNumberConstant(IRContext ctx, String value) {
        char suffix = value.isEmpty() ? '\0'
                : Character.toLowerCase(value.charAt(value.length() - 1));

        String digits = switch (suffix) {
            case 'b', 's', 'l', 'f', 'd' -> value.substring(0, value.length() - 1);
            default -> {
                // 无后缀，优先参考变量类型
                if (ctx.getVarType() != null) {
                    String t = ctx.getVarType();
                    suffix = switch (t) {
                        case "byte" -> 'b';
                        case "short" -> 's';
                        case "int" -> 'i';
                        case "long" -> 'l';
                        case "float" -> 'f';
                        case "double" -> 'd';
                        default -> '\0';
                    };
                }
                yield value;
            }
        };

        // 生成常量对象
        return switch (suffix) {
            case 'b' -> new IRConstant(Byte.parseByte(digits));
            case 's' -> new IRConstant(Short.parseShort(digits));
            case 'l' -> new IRConstant(Long.parseLong(digits));
            case 'f' -> new IRConstant(Float.parseFloat(digits));
            case 'd' -> new IRConstant(Double.parseDouble(digits));
            default -> looksLikeFloat(digits)
                    ? new IRConstant(Double.parseDouble(digits))
                    : new IRConstant(Integer.parseInt(digits));
        };
    }

    // ───────────── 一元运算指令匹配 ─────────────

    /**
     * 根据表达式节点的类型后缀，选择对应的取负（-）运算操作码。
     *
     * @param operand 操作数表达式
     * @return 匹配类型的 IROpCode
     */
    public static IROpCode negOp(ExpressionNode operand) {
        char t = typeChar(operand);
        return switch (t) {
            case 'b' -> IROpCode.NEG_B8;
            case 's' -> IROpCode.NEG_S16;
            case 'l' -> IROpCode.NEG_L64;
            case 'f' -> IROpCode.NEG_F32;
            case 'd' -> IROpCode.NEG_D64;
            default -> IROpCode.NEG_I32;      // 无法推断或为 int
        };
    }

    // ───────────── 比较运算相关 ─────────────

    /**
     * 判断给定字符串是否为支持的比较运算符（==, !=, <, >, <=, >=）。
     *
     * @param op 操作符字符串
     * @return 若为比较运算符返回 true，否则返回 false
     */
    public static boolean isComparisonOperator(String op) {
        return ComparisonUtils.isComparisonOperator(op);
    }

    /**
     * 兼容旧逻辑: 仅凭操作符直接返回 int32 比较指令。
     *
     * @param op 比较操作符
     * @return int32 类型的比较操作码
     */
    public static IROpCode cmpOp(String op) {
        return IROpCodeMappings.CMP_I32.get(op);
    }

    /**
     * 推荐调用: 根据左右表达式类型自动选择 int/long/float/double 等合适的比较操作码。
     *
     * @param variables 变量名到类型的映射
     * @param op        比较符号
     * @param left      左操作数表达式
     * @param right     右操作数表达式
     * @return 匹配类型的比较操作码
     */
    public static IROpCode cmpOp(Map<String, String> variables, String op, ExpressionNode left, ExpressionNode right) {
        return ComparisonUtils.cmpOp(variables, op, left, right);
    }

    // ───────────── 类型推断与算术操作码匹配 ─────────────

    /**
     * 递归推断表达式节点的类型后缀（b/s/i/l/f/d）。
     * 优先从字面量和二元表达式合并类型，变量节点暂不处理，返回 '\0'。
     *
     * @param node 表达式节点
     * @return 推断的类型后缀（小写），不确定时返回 '\0'
     */
    private static char typeChar(ExpressionNode node) {
        if (node instanceof NumberLiteralNode(String value, NodeContext _)) {
            char last = Character.toLowerCase(value.charAt(value.length() - 1));
            return switch (last) {
                case 'b', 's', 'i', 'l', 'f', 'd' -> last;
                default -> looksLikeFloat(value) ? 'd' : '\0';
            };
        }
        if (node instanceof BinaryExpressionNode bin) {
            return maxTypeChar(typeChar(bin.left()), typeChar(bin.right()));
        }
        return '\0';      // 变量等暂不处理
    }

    /**
     * 合并两个表达式节点的类型后缀，按照 d > f > l > i > s > b > '\0' 优先级返回最宽类型。
     *
     * @param left  左表达式
     * @param right 右表达式
     * @return 合并后的类型后缀
     */
    public static char resolveSuffix(ExpressionNode left, ExpressionNode right) {
        return maxTypeChar(typeChar(left), typeChar(right));
    }

    /**
     * 返回两个类型后缀中的最大类型（宽度优先）。
     * 优先级: d > f > l > i > s > b > '\0'
     *
     * @param l 类型后缀1
     * @param r 类型后缀2
     * @return 最宽类型后缀
     */
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
     * 根据操作符和两侧表达式，选择匹配的算术操作码（IROpCode）。
     * <p>
     * 类型推断优先使用左右表达式的类型后缀，推断失败时回退为线程级默认类型后缀，再失败则默认为 int32。
     *
     * @param op    算术操作符
     * @param left  左表达式
     * @param right 右表达式
     * @return 匹配类型的 IROpCode
     */
    public static IROpCode resolveOpCode(String op,
                                         ExpressionNode left,
                                         ExpressionNode right) {
        // 1. 优先根据表达式类型推断
        char suffix = resolveSuffix(left, right);
        // 2. 推断失败则使用线程默认类型
        if (suffix == '\0') suffix = DEFAULT_SUFFIX.get();
        // 3. 仍失败则默认为 int32
        Map<String, IROpCode> table = switch (suffix) {
            case 'b' -> IROpCodeMappings.OP_B8;
            case 's' -> IROpCodeMappings.OP_S16;
            case 'l' -> IROpCodeMappings.OP_L64;
            case 'f' -> IROpCodeMappings.OP_F32;
            case 'd' -> IROpCodeMappings.OP_D64;
            default -> IROpCodeMappings.OP_I32;
        };
        return table.get(op);
    }

    // ───────────── 字符串辅助工具 ─────────────

    /**
     * 判断字面量字符串是否看起来像浮点数（包含小数点或 e/E 科学计数法）。
     *
     * @param digits 字面量字符串
     * @return 是浮点格式则返回 true
     */
    private static boolean looksLikeFloat(String digits) {
        return digits.indexOf('.') >= 0
                || digits.indexOf('e') >= 0
                || digits.indexOf('E') >= 0;
    }
}
