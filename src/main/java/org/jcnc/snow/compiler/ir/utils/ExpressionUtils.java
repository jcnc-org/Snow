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
 * <p>
 * 主要功能包括：
 * <ul>
 *   <li>字面量常量的解析与类型推断</li>
 *   <li>自动匹配操作码</li>
 *   <li>表达式类型合并与判定</li>
 * </ul>
 */
public class ExpressionUtils {

    /** 用于存储默认的类型后缀（如函数返回类型），线程隔离。 */
    private static final ThreadLocal<Character> DEFAULT_SUFFIX =
            ThreadLocal.withInitial(() -> '\0');

    /**
     * 在进入函数 IR 构建前设置默认的类型后缀（比如函数返回类型）。
     * @param suffix 默认后缀字符，如 'i', 'l', 'f', 'd' 等
     */
    public static void setDefaultSuffix(char suffix) {
        DEFAULT_SUFFIX.set(suffix);
    }

    /**
     * 在函数 IR 构建结束后清除默认后缀，避免影响后续分析。
     */
    public static void clearDefaultSuffix() {
        DEFAULT_SUFFIX.set('\0');
    }

    /**
     * 解析整数字面量字符串，自动去除类型后缀（b/s/l/f/d/B/S/L/F/D），并转换为 int。
     *
     * @param literal 字面量字符串，如 "123", "123l", "42B"
     * @return 解析得到的 int 整数
     */
    public static int parseIntSafely(String literal) {
        // 去掉类型后缀，只保留数字部分
        String digits = literal.replaceAll("[bslfdBSDLF]$", "");
        return Integer.parseInt(digits);
    }

    /**
     * 根据数字字面量字符串自动判断类型，生成对应类型的 IRConstant。
     * 支持 b/s/l/f/d 类型后缀与浮点格式，自动分配合适类型。
     *
     * @param ctx IR 编译上下文环境
     * @param value 字面量字符串（如 "1", "2l", "3.14f", "5D"）
     * @return 生成的 IRConstant 对象，包含正确类型
     */
    public static IRConstant buildNumberConstant(IRContext ctx, String value) {
        char suffix = value.isEmpty() ? '\0' : Character.toLowerCase(value.charAt(value.length() - 1));
        String digits = switch (suffix) {
            case 'b','s','l','f','d' -> value.substring(0, value.length() - 1);
            default -> {
                if (ctx.getVarType().isPresent()) {
                    final var receiverType = ctx.getVarType().get();
                    switch (receiverType) {
                        case "byte" -> suffix = 'b';
                        case "short" -> suffix = 's';
                        case "int" -> suffix = 'i';
                        case "long" -> suffix = 'l';
                        case "float" -> suffix = 'f';
                        case "double" -> suffix = 'd';
                    }
                }

                yield value;
            }
        };
        // 根据类型后缀或数值格式创建常量
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

    /**
     * 根据表达式节点推断一元取负（-）运算应使用的操作码。
     *
     * <p>优先级与 {@link #resolveOpCode} 使用的类型提升规则保持一致：</p>
     * <ul>
     *   <li>字面量或标识符带显式后缀时，直接以后缀决定位宽；</li>
     *   <li>未显式指定时，默认使用 32 位整型 {@link IROpCode#NEG_I32}。</li>
     * </ul>
     *
     * @param operand 一元取负运算的操作数
     * @return 匹配的 {@link IROpCode}
     */
    public static IROpCode negOp(ExpressionNode operand) {
        char t = typeChar(operand);
        return switch (t) {
            case 'b' -> IROpCode.NEG_B8;
            case 's' -> IROpCode.NEG_S16;
            case 'l' -> IROpCode.NEG_L64;
            case 'f' -> IROpCode.NEG_F32;
            case 'd' -> IROpCode.NEG_D64;
            default  -> IROpCode.NEG_I32;
        };
    }

    /* =================== 类型推断与操作符匹配 =================== */

    /**
     * 递归推断单个表达式节点的类型后缀（b/s/l/f/d）。
     * 对于二元表达式，将左右两侧的类型自动提升合并，遵循优先级顺序：d > f > l > s > b > '\0'。
     *
     * @param node 表达式节点
     * @return 类型后缀字符，b/s/l/f/d 或 '\0'
     */
    private static char typeChar(ExpressionNode node) {
        // 字面量节点，直接判断最后一位
        if (node instanceof NumberLiteralNode(String value)) {
            char last = Character.toLowerCase(value.charAt(value.length() - 1));
            return switch (last) {
                case 'b', 's', 'l', 'f', 'd' -> last;
                default -> looksLikeFloat(value) ? 'd' : '\0';
            };
        }
        // 二元表达式，递归判断左右子节点
        if (node instanceof BinaryExpressionNode bin) {
            char l = typeChar(bin.left());
            char r = typeChar(bin.right());
            return maxTypeChar(l, r);
        }
        // 其他情况（如变量节点），暂不处理，默认返回 '\0'
        return '\0';
    }

    /**
     * 推断两个表达式节点合并后的类型后缀。
     * 返回优先级更高的类型后缀字符。
     *
     * @param left  左表达式节点
     * @param right 右表达式节点
     * @return 合并后类型的后缀字符
     */
    public static char resolveSuffix(ExpressionNode left, ExpressionNode right) {
        return maxTypeChar(typeChar(left), typeChar(right));
    }

    /**
     * 在两个类型后缀中选取精度更高的一个。
     * 优先级依次为：d > f > l > s > b > '\0'
     *
     * @param l 左类型后缀
     * @param r 右类型后缀
     * @return 更高优先级的类型后缀字符
     */
    private static char maxTypeChar(char l, char r) {
        if (l == 'd' || r == 'd') return 'd';
        if (l == 'f' || r == 'f') return 'f';
        if (l == 'l' || r == 'l') return 'l';
        if (l == 's' || r == 's') return 's';
        if (l == 'b' || r == 'b') return 'b';
        return '\0';
    }

    /**
     * 判断给定字符串是否为比较运算符（如 >, <, == 等）。
     *
     * @param op 操作符字符串
     * @return 如果是比较操作符返回 true，否则返回 false
     */
    public static boolean isComparisonOperator(String op) {
        return IROpCodeMappings.CMP.containsKey(op);
    }

    /**
     * 获取比较操作符对应的中间表示操作码（IROpCode）。
     *
     * @param op 比较操作符字符串
     * @return 对应的 IROpCode，如果不存在则返回 null
     */
    public static IROpCode cmpOp(String op) {
        return IROpCodeMappings.CMP.get(op);
    }

    /**
     * 根据操作符和两侧表达式自动选择正确的 IROpCode。
     * 首先根据参与表达式类型推断后缀，若无法推断则回退到函数默认类型，
     * 还无法推断则默认使用 i32（32位整型）。
     *
     * @param op 操作符字符串，如 "+"
     * @param left 左侧表达式节点
     * @param right 右侧表达式节点
     * @return 匹配的 IROpCode，如果不存在则返回 null
     */
    public static IROpCode resolveOpCode(String op,
                                         ExpressionNode left,
                                         ExpressionNode right) {

        /* 1) 尝试从参与者常量字面量推断 */
        char suffix = resolveSuffix(left, right);

        /* 2) 若无法推断，退回到函数返回类型（DEFAULT_SUFFIX） */
        if (suffix == '\0') {
            suffix = DEFAULT_SUFFIX.get();
        }

        /* 3) 再次失败则默认为 i32 */
        Map<String, IROpCode> table = switch (suffix) {
            case 'b' -> IROpCodeMappings.OP_I8;
            case 's' -> IROpCodeMappings.OP_I16;
            case 'l' -> IROpCodeMappings.OP_L64;
            case 'f' -> IROpCodeMappings.OP_F32;
            case 'd' -> IROpCodeMappings.OP_D64;
            default  -> IROpCodeMappings.OP_I32;
        };

        return table.get(op);
    }

    /**
     * 判断字符串是否为浮点数形式（即包含小数点或科学计数法 e/E）。
     *
     * @param digits 数字字符串
     * @return 如果看起来像浮点数则返回 true，否则返回 false
     */
    private static boolean looksLikeFloat(String digits) {
        return digits.indexOf('.') >= 0 || digits.indexOf('e') >= 0 || digits.indexOf('E') >= 0;
    }
}
