package org.jcnc.snow.compiler.semantic.analyzers.expression;

import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.parser.ast.NumberLiteralNode;
import org.jcnc.snow.compiler.semantic.analyzers.base.ExpressionAnalyzer;
import org.jcnc.snow.compiler.semantic.core.Context;
import org.jcnc.snow.compiler.semantic.core.ModuleInfo;
import org.jcnc.snow.compiler.semantic.symbol.SymbolTable;
import org.jcnc.snow.compiler.semantic.type.BuiltinType;
import org.jcnc.snow.compiler.semantic.type.Type;

/**
 * {@code NumberLiteralAnalyzer} 用于对数字字面量表达式进行语义分析并推断其精确类型。
 * <p>
 * 类型判定逻辑如下：
 * <ol>
 *   <li>首先检查字面量末尾是否带有类型后缀（不区分大小写）：
 *     <ul>
 *       <li>{@code b}：byte 类型（{@link BuiltinType#BYTE}）</li>
 *       <li>{@code s}：short 类型（{@link BuiltinType#SHORT}）</li>
 *       <li>{@code l}：long 类型（{@link BuiltinType#LONG}）</li>
 *       <li>{@code f}：float 类型（{@link BuiltinType#FLOAT}）</li>
 *       <li>{@code d}：double 类型（{@link BuiltinType#DOUBLE}）</li>
 *     </ul>
 *   </li>
 *   <li>若无后缀，则：
 *     <ul>
 *       <li>只要文本中包含 {@code '.'} 或 {@code e/E}，即判为 double 类型</li>
 *       <li>否则默认判为 int 类型</li>
 *     </ul>
 *   </li>
 * </ol>
 * 本分析器不处理溢出、非法格式等诊断，仅做类型推断。
 */
public class NumberLiteralAnalyzer implements ExpressionAnalyzer<NumberLiteralNode> {

    /**
     * 对数字字面量进行语义分析，推断其类型。
     * <p>
     * 分析流程：
     * <ol>
     *   <li>若字面量以后缀结尾，直接按后缀映射类型。</li>
     *   <li>否则，若含有小数点或科学计数法标记，则为 double，否则为 int。</li>
     * </ol>
     *
     * @param ctx    当前语义分析上下文（可用于记录诊断信息等，当前未使用）
     * @param mi     当前模块信息（未使用）
     * @param fn     当前函数节点（未使用）
     * @param locals 当前作用域的符号表（未使用）
     * @param expr   数字字面量表达式节点
     * @return 对应的 {@link BuiltinType}，如 INT、DOUBLE 等
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
            // 理论上不应为空，兜底返回 int 类型
            return BuiltinType.INT;
        }

        // 获取最后一个字符，判断是否为类型后缀（b/s/l/f/d，忽略大小写）
        char lastChar = raw.charAt(raw.length() - 1);
        char suffix = Character.toLowerCase(lastChar);
        boolean hasSuffix = switch (suffix) {
            case 'b', 's', 'l', 'f', 'd' -> true;
            default -> false;
        };

        // 若有后缀，则 digits 为去除后缀的数字部分，否则为原文本
        String digits = hasSuffix ? raw.substring(0, raw.length() - 1) : raw;

        // 1. 若有后缀，直接返回对应类型
        if (hasSuffix) {
            return switch (suffix) {
                case 'b' -> BuiltinType.BYTE;
                case 's' -> BuiltinType.SHORT;
                case 'l' -> BuiltinType.LONG;
                case 'f' -> BuiltinType.FLOAT;
                case 'd' -> BuiltinType.DOUBLE;
                default -> BuiltinType.INT; // 理论上不会到这里
            };
        }

        // 2. 无后缀，根据文本是否含小数点或科学计数法（e/E）判断类型
        if (digits.indexOf('.') >= 0 || digits.indexOf('e') >= 0 || digits.indexOf('E') >= 0) {
            return BuiltinType.DOUBLE; // 有小数/科学计数，默认 double 类型
        }

        return BuiltinType.INT; // 否则为纯整数，默认 int 类型
    }
}
