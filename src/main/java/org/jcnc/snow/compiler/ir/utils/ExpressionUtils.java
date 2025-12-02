package org.jcnc.snow.compiler.ir.utils;

import org.jcnc.snow.common.NumberLiteralHelper;
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
 *
 * <p>主要职责包括：</p>
 * <ul>
 *     <li>解析数字字面量并自动推断对应的常量类型</li>
 *     <li>根据表达式类型自动匹配算术运算指令与比较运算指令</li>
 *     <li>表达式类型推断与类型合并（类型提升）</li>
 *     <li>提供线程隔离的默认类型后缀，用于类型推断兜底</li>
 * </ul>
 *
 * <p>该类为纯工具类，不允许实例化。</p>
 */
public final class ExpressionUtils {

    /**
     * 当前线程的默认类型后缀（如当前函数返回类型等），用于类型推断兜底。
     * 可能的取值为：i/l/f/d，或 '\0' 表示未设置。
     */
    private static final ThreadLocal<Character> DEFAULT_SUFFIX =
            ThreadLocal.withInitial(() -> '\0');

    private ExpressionUtils() {
    }

    /**
     * 设置当前线程的默认类型后缀。
     *
     * @param suffix 类型后缀字符（i/l/f/d），'\0' 表示未设置
     */
    public static void setDefaultSuffix(char suffix) {
        DEFAULT_SUFFIX.set(suffix);
    }

    /**
     * 清除当前线程的默认类型后缀，将其重置为 '\0'。
     */
    public static void clearDefaultSuffix() {
        DEFAULT_SUFFIX.set('\0');
    }

    /**
     * 构建数字字面量常量，并带有位置信息（用于错误提示）。
     *
     * @param ctx     IR 上下文
     * @param value   字面量文本
     * @param nodeCtx 节点位置信息
     * @return 常量值 IRConstant
     */
    public static IRConstant buildNumberConstant(IRContext ctx, String value, NodeContext nodeCtx) {
        if (value == null || value.isEmpty()) {
            return new IRConstant(0);
        }

        char suffix = NumberLiteralHelper.extractTypeSuffix(value);
        boolean hasLetterSuffix = suffix != '\0';
        boolean explicitSuffix = suffix == 'l' || suffix == 'f' || suffix == 'd';

        NumberLiteralHelper.NormalizedLiteral normalized =
                NumberLiteralHelper.normalize(value, explicitSuffix || hasLetterSuffix);

        String digits = normalized.text();
        String payload = normalized.digits();
        int radix = normalized.radix();

        // 若无显式后缀，尝试根据上下文类型推断
        if (!explicitSuffix && ctx.getVarType() != null) {
            suffix = switch (ctx.getVarType()) {
                case "byte" -> 'b';
                case "short" -> 's';
                case "int" -> 'i';
                case "long" -> 'l';
                case "float" -> 'f';
                case "double" -> 'd';
                default -> '\0';
            };
        }

        // 若仍无类型后缀，根据字面量特征自动推断
        if (!explicitSuffix && suffix == '\0') {
            suffix = NumberLiteralHelper.looksLikeFloat(digits) ? 'd' : 'i';
        }

        try {
            return switch (suffix) {
                case 'b' -> new IRConstant(parseByteLiteral(payload, radix));
                case 's' -> new IRConstant(parseShortLiteral(payload, radix));
                case 'i' -> new IRConstant(parseIntLiteral(payload, radix));
                case 'l' -> new IRConstant(parseLongLiteral(payload, radix));
                case 'f' -> new IRConstant(parseFloatLiteral(digits, payload, radix));
                case 'd' -> new IRConstant(parseDoubleLiteral(digits, payload, radix));
                default -> new IRConstant(parseIntLiteral(payload, radix));
            };
        } catch (NumberFormatException ex) {
            throw literalFormatError(value, nodeCtx);
        }
    }

    // 各种类型的具体解析逻辑
    private static byte parseByteLiteral(String payload, int radix) {
        if (radix == 16) {
            int v = Integer.parseUnsignedInt(payload, radix);
            if (v > 0xFF) throw new NumberFormatException("byte literal overflow");
            return (byte) v;
        }
        return Byte.parseByte(payload);
    }

    private static short parseShortLiteral(String payload, int radix) {
        if (radix == 16) {
            int v = Integer.parseUnsignedInt(payload, radix);
            if (v > 0xFFFF) throw new NumberFormatException("short literal overflow");
            return (short) v;
        }
        return Short.parseShort(payload);
    }

    private static int parseIntLiteral(String payload, int radix) {
        return NumberLiteralHelper.parseIntLiteral(payload, radix);
    }

    private static long parseLongLiteral(String payload, int radix) {
        return NumberLiteralHelper.parseLongLiteral(payload, radix);
    }

    private static float parseFloatLiteral(String digits, String payload, int radix) {
        if (radix == 16) {
            return (float) Long.parseUnsignedLong(payload, radix);
        }
        return Float.parseFloat(digits);
    }

    private static double parseDoubleLiteral(String digits, String payload, int radix) {
        if (radix == 16) {
            return (double) Long.parseUnsignedLong(payload, radix);
        }
        return Double.parseDouble(digits);
    }

    /**
     * 根据表达式类型选择适配的取负（-）操作码。
     *
     * @param operand 操作数表达式
     * @return 对应类型的 IROpCode
     */
    public static IROpCode negOp(ExpressionNode operand) {
        char t = typeChar(operand);
        return switch (t) {
            case 'l' -> IROpCode.NEG_L64;
            case 'f' -> IROpCode.NEG_F32;
            case 'd' -> IROpCode.NEG_D64;
            default -> IROpCode.NEG_I32; // 默认使用 int32
        };
    }

    /**
     * 递归推断表达式节点的类型后缀（i/l/f/d）。
     *
     * <p>规则：</p>
     * <ul>
     *     <li>数字字面量：根据后缀/格式推断类型</li>
     *     <li>二元表达式：从左右操作数合并类型</li>
     *     <li>变量节点：不进行推断，返回 '\0'</li>
     * </ul>
     *
     * @param node 表达式节点
     * @return 推断出的类型后缀
     */
    private static char typeChar(ExpressionNode node) {
        if (node instanceof NumberLiteralNode(String value, NodeContext _)) {
            char suffix = NumberLiteralHelper.extractTypeSuffix(value);
            if (suffix == 'l' || suffix == 'f' || suffix == 'd') {
                return suffix;
            }
            return NumberLiteralHelper.looksLikeFloat(value) ? 'd' : 'i';
        }
        if (node instanceof BinaryExpressionNode bin) {
            return maxTypeChar(typeChar(bin.left()), typeChar(bin.right()));
        }
        return '\0';
    }

    /**
     * 合并左右表达式的类型后缀，选择最宽的类型。
     *
     * <p>优先级：d > f > l > i > s > b > '\0'</p>
     */
    public static char resolveSuffix(ExpressionNode left, ExpressionNode right) {
        return maxTypeChar(typeChar(left), typeChar(right));
    }

    /**
     * 返回两个类型后缀中“更宽”的一个。
     *
     * @param l 左类型后缀
     * @param r 右类型后缀
     * @return 最宽类型后缀
     */
    private static char maxTypeChar(char l, char r) {
        l = normalizeIntegralSuffix(l);
        r = normalizeIntegralSuffix(r);
        if (l == 'd' || r == 'd') return 'd';
        if (l == 'f' || r == 'f') return 'f';
        if (l == 'l' || r == 'l') return 'l';
        if (l == 'i' || r == 'i') return 'i';
        return '\0';
    }

    private static char normalizeIntegralSuffix(char c) {
        return (c == 'b' || c == 's' || c == 'i') ? 'i' : c;
    }

    /**
     * 根据操作符与表达式类型自动选择算术运算操作码。
     *
     * <p>选择顺序：</p>
     * <ol>
     *     <li>根据左右表达式类型推断</li>
     *     <li>若无法推断，则使用线程级默认类型后缀</li>
     *     <li>仍无法确定则默认为 int32 表达式运算表</li>
     * </ol>
     *
     * @param op    算术操作符
     * @param left  左表达式
     * @param right 右表达式
     * @return 对应类型的 IROpCode
     */
    public static IROpCode resolveOpCode(String op,
                                         ExpressionNode left,
                                         ExpressionNode right) {

        char suffix = resolveSuffix(left, right);

        if (suffix == '\0') suffix = DEFAULT_SUFFIX.get();
        suffix = normalizeIntegralSuffix(suffix);

        Map<String, IROpCode> table = switch (suffix) {
            case 'l' -> IROpCodeMappings.OP_L64;
            case 'f' -> IROpCodeMappings.OP_F32;
            case 'd' -> IROpCodeMappings.OP_D64;
            case 'i' -> IROpCodeMappings.OP_I32;
            default -> IROpCodeMappings.OP_I32;
        };

        return table.get(op);
    }

    /**
     * 按 Snow 语言错误格式包装数字字面量格式错误。
     *
     * @param value 原始字面量
     * @param ctx   位置信息
     * @return 异常对象
     */
    private static RuntimeException literalFormatError(String value, NodeContext ctx) {
        String loc;
        if (ctx != null && ctx.file() != null && !ctx.file().isBlank()) {
            loc = "file:///" + ctx.file() + ":" + ctx.line() + ":" + ctx.column();
        } else {
            loc = "file:///?:0:0";
        }

        String msg = "语法错误: 解析过程中检测到 1 处错误:\n"
                + " - " + loc + ": 非法数字字面量: " + value;

        return new IllegalStateException(msg);
    }
}