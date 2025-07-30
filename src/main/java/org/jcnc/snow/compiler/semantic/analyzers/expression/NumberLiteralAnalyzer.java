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
 * {@code NumberLiteralAnalyzer} 用于对数字字面量表达式进行语义分析并推断其精确类型，并支持类型范围校验与多错误收集。
 * <p>
 * 类型推断规则如下：
 * <ol>
 *   <li>若字面量以类型后缀（b/s/l/f/d，大小写均可）结尾，则按后缀直接推断目标类型。</li>
 *   <li>若无后缀，且文本包含小数点或科学计数法（'.' 或 'e/E'），则推断为 double 类型。</li>
 *   <li>否则推断为 int 类型。</li>
 * </ol>
 * <p>
 * 校验规则如下：
 * <ul>
 *   <li>按推断类型解析字符串，若超出可表示范围或格式非法，则向语义上下文添加 {@link SemanticError}，不抛异常，支持多错误收集和 IDE 友好提示。</li>
 *   <li>所有建议信息风格统一，指引用户用更大类型的后缀或类型声明，最大类型提示为 double。</li>
 * </ul>
 */
public class NumberLiteralAnalyzer implements ExpressionAnalyzer<NumberLiteralNode> {

    /**
     * 根据字面量后缀和文本内容推断类型。
     *
     * @param hasSuffix 是否有类型后缀
     * @param suffix    类型后缀字符
     * @param digits    去除后缀的纯数字字符串
     * @return 推断出的类型
     */
    private static Type inferType(boolean hasSuffix, char suffix, String digits) {
        if (hasSuffix) {
            return switch (suffix) {
                case 'b' -> BYTE;
                case 's' -> SHORT;
                case 'l' -> BuiltinType.LONG;
                case 'f' -> BuiltinType.FLOAT;
                case 'd' -> BuiltinType.DOUBLE;
                default -> INT;
            };
        }
        if (digits.indexOf('.') >= 0 || digits.indexOf('e') >= 0 || digits.indexOf('E') >= 0) {
            return BuiltinType.DOUBLE;
        }
        return INT;
    }

    /**
     * 检查字面量数值是否在推断类型范围内，超出范围则添加语义错误。
     *
     * @param ctx      语义分析上下文
     * @param node     当前字面量节点
     * @param raw      字面量原始字符串（含后缀）
     * @param inferred 推断类型
     * @param digits   去除后缀的纯数字部分
     */
    private static void validateRange(Context ctx,
                                      NumberLiteralNode node,
                                      String raw,
                                      Type inferred,
                                      String digits) {
        try {
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
            } else if (inferred == BuiltinType.FLOAT) {
                float v = Float.parseFloat(digits);
                if (Float.isInfinite(v)) throw new NumberFormatException(digits);
            } else if (inferred == BuiltinType.DOUBLE) {
                double v = Double.parseDouble(digits);
                if (Double.isInfinite(v)) throw new NumberFormatException(digits);
            }
        } catch (NumberFormatException ex) {
            String typeName = switch (inferred) {
                case BYTE -> "byte";
                case SHORT -> "short";
                case INT -> "int";
                case LONG -> "long";
                case FLOAT -> "float";
                case DOUBLE -> "double";
                default -> inferred.toString();
            };

            String msg = getString(raw, digits, typeName);
            ctx.getErrors().add(new SemanticError(node, msg));
            ctx.log("错误: " + msg);
        }
    }

    /**
     * 根据类型名生成风格统一的溢出建议字符串。
     *
     * @param raw      字面量原始字符串
     * @param digits   去除后缀的数字部分
     * @param typeName 推断类型的名称
     * @return 完整的错误描述
     */
    private static String getString(String raw, String digits, String typeName) {
        String suggestion = switch (typeName) {
            case "int" ->
                    "如需更大范围，请在数字末尾添加大写字母 'L'（如 " + digits + "L），或将变量类型声明为 long。";
            case "long" ->
                    "long 已为整数的最大类型，如需更大范围，请将变量类型声明为 double（注意精度）。";
            case "float" ->
                    "如需更大范围，请将变量类型声明为 double。";
            case "byte" ->
                    "如需更大范围，请在数字末尾添加大写字母 'S'（如 " + digits + "S），或将变量类型声明为 short、int 或 long。";
            case "short" ->
                    "如需更大范围，请在数字末尾添加大写字母 'L'（如 " + digits + "L），或将变量类型声明为 int 或 long。";
            case "double" ->
                    "double 已为数值类型的最大范围，请缩小数值。";
            default ->
                    "请调整字面量类型或范围。";
        };
        return "数值字面量越界: \"" + raw + "\" 超出 " + typeName + " 可表示范围。 " + suggestion;
    }

    /**
     * 判断字符串是否为浮点数格式（包含小数点或科学计数法）。
     *
     * @param s 数字部分字符串
     * @return 是否为浮点格式
     */
    private static boolean looksLikeFloat(String s) {
        return s.indexOf('.') >= 0 || s.indexOf('e') >= 0 || s.indexOf('E') >= 0;
    }

    /**
     * 对数字字面量进行语义分析，推断类型，并进行溢出与格式校验。
     *
     * @param ctx    当前语义分析上下文（可用于错误收集等）
     * @param mi     当前模块信息（未使用）
     * @param fn     当前函数节点（未使用）
     * @param locals 当前作用域的符号表（未使用）
     * @param expr   数字字面量表达式节点
     * @return 推断出的类型
     */
    @Override
    public Type analyze(Context ctx,
                        ModuleInfo mi,
                        FunctionNode fn,
                        SymbolTable locals,
                        NumberLiteralNode expr) {

        // 获取字面量原始文本（如 "123", "3.14", "2f" 等）
        String raw = expr.value();
        if (raw == null || raw.isEmpty()) {
            return INT;
        }

        // 判断并去除后缀
        char lastChar = raw.charAt(raw.length() - 1);
        char suffix = Character.toLowerCase(lastChar);
        boolean hasSuffix = switch (suffix) {
            case 'b', 's', 'l', 'f', 'd' -> true;
            default -> false;
        };
        String digits = hasSuffix ? raw.substring(0, raw.length() - 1) : raw;

        // 推断类型
        Type inferred = inferType(hasSuffix, suffix, digits);

        // 做范围校验，发现溢出等问题直接加到 ctx 错误列表
        validateRange(ctx, expr, raw, inferred, digits);

        return inferred;
    }
}
