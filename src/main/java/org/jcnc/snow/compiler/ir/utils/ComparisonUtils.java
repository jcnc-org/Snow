package org.jcnc.snow.compiler.ir.utils;

import org.jcnc.snow.compiler.ir.core.IROpCode;
import org.jcnc.snow.compiler.ir.core.IROpCodeMappings;
import org.jcnc.snow.compiler.parser.ast.IdentifierNode;
import org.jcnc.snow.compiler.parser.ast.NumberLiteralNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;

import java.util.Map;

/**
 * 比较运算辅助工具：
 * 根据左右操作数类型（目前通过字面量后缀 <code>L/l</code> 判定）选择
 * 正确的 IR 比较指令，保证 int/long 均能正常运行。
 */
public final class ComparisonUtils {
    private ComparisonUtils() {
    }

    /**
     * 判断给定操作符是否为比较运算符
     */
    public static boolean isComparisonOperator(String op) {
        // 两张表 key 完全一致，只需检查一张
        return IROpCodeMappings.CMP_I32.containsKey(op);
    }

    /**
     * <b>类型宽度优先级</b>：D > F > L > I > S > B
     * <ul>
     *     <li>D（double）：6</li>
     *     <li>F（float）：5</li>
     *     <li>L（long）：4</li>
     *     <li>I（int）：3</li>
     *     <li>S（short）：2</li>
     *     <li>B（byte）：1</li>
     *     <li>未识别类型：0</li>
     * </ul>
     *
     * @param p 类型标记字符
     * @return 优先级数值（越大类型越宽）
     */
    private static int rank(char p) {
        return switch (p) {
            case 'D' -> 6;
            case 'F' -> 5;
            case 'L' -> 4;
            case 'I' -> 3;
            case 'S' -> 2;
            case 'B' -> 1;
            default -> 0;
        };
    }

    /**
     * 返回更“宽”的公共类型（即优先级高的类型）。
     *
     * @param a 类型标记字符 1
     * @param b 类型标记字符 2
     * @return 宽度更高的类型标记字符
     */
    public static char promote(char a, char b) {
        return rank(a) >= rank(b) ? a : b;
    }

    /**
     * 返回符合操作数位宽的比较 IROpCode。
     *
     * @param variables  变量->类型的映射
     * @param op         比较符号（==, !=, <, >, <=, >=）
     * @param left       左操作数 AST
     * @param right      右操作数 AST
     */
    public static IROpCode cmpOp(Map<String, String> variables, String op, ExpressionNode left, ExpressionNode right) {
        char typeLeft = analysisType(variables, left);
        char typeRight = analysisType(variables, right);
        char type = promote(typeLeft, typeRight);

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

    /* ------------ 内部工具 ------------ */

    private static char analysisType(Map<String, String> variables, ExpressionNode node) {
        if (node instanceof NumberLiteralNode(String value, NodeContext _)) {
            char suffix = Character.toUpperCase(value.charAt(value.length() - 1));
            if ("BSILFD".indexOf(suffix) != -1) {
                return suffix;
            }
            if (value.indexOf('.') != -1) {
                return 'D';
            }

            return 'I';  // 默认为 'I'
        }
        if (node instanceof IdentifierNode(String name, NodeContext _)) {
            final String type = variables.get(name);
            switch (type) {
                case "byte" -> {
                    return 'B';
                }
                case "short" -> {
                    return 'S';
                }
                case "int" -> {
                    return 'I';
                }
                case "long" -> {
                    return 'L';
                }
                case "float" -> {
                    return 'F';
                }
                case "double" -> {
                    return 'D';
                }
            }
        }

        return 'I'; // 默认为 'I'
    }
}
