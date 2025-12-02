package org.jcnc.snow.compiler.ir.utils;

import org.jcnc.snow.common.NumberLiteralHelper;
import org.jcnc.snow.compiler.ir.core.IROpCode;
import org.jcnc.snow.compiler.ir.core.IROpCodeMappings;
import org.jcnc.snow.compiler.parser.ast.BinaryExpressionNode;
import org.jcnc.snow.compiler.parser.ast.IdentifierNode;
import org.jcnc.snow.compiler.parser.ast.NumberLiteralNode;
import org.jcnc.snow.compiler.parser.ast.StringLiteralNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;

import java.util.Map;

/**
 * 工具类，用于比较运算相关的类型推断和指令选择。
 * <p>
 * 该类主要用于根据左右操作数的静态类型，自动选择正确的 IR 层比较操作码。
 * 支持自动类型提升，保证 int、long、float、double 等类型的比较均能得到正确的 IR 指令。
 * </p>
 * <p>
 * 类型判定支持:
 * <ul>
 *     <li>字面量后缀: 识别 L/F/D（整数字面量默认 int）</li>
 *     <li>浮点数支持: 如无后缀但有小数点，视为 double</li>
 *     <li>变量类型: 根据变量表推断，byte/short 视为 int，未识别则默认 int</li>
 * </ul>
 */
public final class ComparisonUtils {
    private ComparisonUtils() {
    }

    /**
     * 判断给定字符串是否为受支持的比较运算符（==, !=, <, >, <=, >=）。
     * 仅检查 int 类型指令表，因所有类型的比较符号集合相同。
     *
     * @param op 比较运算符字符串
     * @return 若为比较运算符返回 true，否则返回 false
     */
    public static boolean isComparisonOperator(String op) {
        // 只需查 int 类型表即可
        return IROpCodeMappings.CMP_I32.containsKey(op) ||
                IROpCodeMappings.CMP_R.containsKey(op);
    }

    /**
     * 返回类型宽度优先级（越大代表类型越宽）。类型对应的优先级:
     * - D (double): 6
     * - F (float): 5
     * - L (long): 4
     * - I (int): 3
     * - S (short): 2
     * - B (byte): 1
     * - 未知类型: 0
     *
     * @param p 类型标记字符
     * @return 类型优先级数值
     */
    private static int rank(char p) {
        return switch (p) {
            case 'D' -> 6;
            case 'F' -> 5;
            case 'L' -> 4;
            case 'I', 'S', 'B' -> 3; // byte/short 按 int 处理
            case 'R' -> 7;
            default -> 0;
        };
    }

    /**
     * 返回两个类型中较“宽”的类型（即优先级较高的类型）。
     * 若优先级相等，返回第一个参数的类型。
     *
     * @param a 类型标记字符1
     * @param b 类型标记字符2
     * @return 宽度更高的类型字符
     */
    public static char promote(char a, char b) {
        return rank(a) >= rank(b) ? a : b;
    }

    /**
     * 根据变量类型映射和操作数表达式，推断操作数类型，
     * 并自动类型提升后，选择正确的比较操作码（IROpCode）。
     * 若未能推断类型或操作符不受支持，会抛出异常。
     *
     * @param variables 变量名到类型的映射（如 "a" -> "int"）
     * @param op        比较符号（==, !=, <, >, <=, >=）
     * @param left      左操作数表达式
     * @param right     右操作数表达式
     * @return 适用的比较 IROpCode
     * @throws IllegalStateException 如果无法推断合适的类型
     */
    public static IROpCode cmpOp(Map<String, String> variables, String op, ExpressionNode left, ExpressionNode right) {
        char typeLeft = analysisType(variables, left);
        char typeRight = analysisType(variables, right);
        char type = promote(typeLeft, typeRight);

        if (type == 'R') {
            if (!IROpCodeMappings.CMP_R.containsKey(op)) {
                throw new IllegalStateException("Unsupported reference comparison operator: " + op);
            }
            return IROpCodeMappings.CMP_R.get(op);
        }

        Map<String, IROpCode> table = switch (type) {
            case 'B' -> IROpCodeMappings.CMP_B8;
            case 'S' -> IROpCodeMappings.CMP_S16;
            case 'I' -> IROpCodeMappings.CMP_I32;
            case 'L' -> IROpCodeMappings.CMP_L64;
            case 'F' -> IROpCodeMappings.CMP_F32;
            case 'D' -> IROpCodeMappings.CMP_D64;
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };

        return table.get(op);
    }

    /**
     * 内部工具方法: 根据表达式节点和变量表推断类型标记字符。
     * 字面量支持 B/S/I/L/F/D（大小写均可），浮点数默认 double；
     * 标识符类型按变量表映射，未知则默认 int。
     *
     * @param variables 变量名到类型的映射
     * @param node      表达式节点
     * @return 类型标记字符（B/S/I/L/F/D），未知时返回 I
     */
    private static char analysisType(Map<String, String> variables, ExpressionNode node) {
        if (node instanceof BinaryExpressionNode bin) {
            if ("+".equals(bin.operator()) &&
                    (bin.left() instanceof StringLiteralNode || bin.right() instanceof StringLiteralNode)) {
                return 'R';
            }
        }
        if (node instanceof NumberLiteralNode(String value, NodeContext _)) {
            char suffix = NumberLiteralHelper.extractTypeSuffix(value);
            return switch (suffix) {
                case 'l' -> 'L';
                case 'f' -> 'F';
                case 'd' -> 'D';
                default -> (NumberLiteralHelper.looksLikeFloat(value) ? 'D' : 'I'); // 默认 int
            };
        }
        if (node instanceof StringLiteralNode) {
            return 'R';
        }
        if (node instanceof IdentifierNode(String name, NodeContext _)) {
            final String type = variables.get(name);
            if (type != null) {
                switch (type) {
                    case "byte":
                    case "short":
                    case "int":
                        return 'I';
                    case "long":
                        return 'L';
                    case "float":
                        return 'F';
                    case "string":
                        return 'R';
                    case "double":
                        return 'D';
                }
            }
        }
        return 'I'; // 默认 int
    }
}