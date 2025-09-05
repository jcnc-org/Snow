package org.jcnc.snow.compiler.ir.builder.statement.utils;

import org.jcnc.snow.compiler.ir.builder.statement.StatementBuilderContext;
import org.jcnc.snow.compiler.parser.ast.*;

import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 常量折叠与常量读取工具。
 * <p>
 * 用于在IR生成阶段，对表达式节点尝试进行常量折叠（即在编译期求值），
 * 并支持从当前作用域读取已知常量。常用于表达式优化、控制流静态分析等场景。
 * </p>
 * <ul>
 *     <li>支持基础字面量类型（数字、字符串、布尔、数组）和常量变量读取。</li>
 *     <li>失败或遇到非常量时返回 {@code null}，不会抛出异常。</li>
 * </ul>
 */
public record ConstFolder(StatementBuilderContext ctx) {

    /**
     * 尝试对表达式节点进行常量折叠（即编译期求值）。
     *
     * @param expr 需要折叠的表达式节点
     * @return 如果能静态求值则返回常量对象（可为Integer、Double、String、List等），否则返回null
     */
    public Object tryFoldConst(ExpressionNode expr) {
        switch (expr) {
            case null -> {
                // 空表达式恒为null
                return null;
            }
            case NumberLiteralNode n -> {
                // 处理数字字面量（自动区分整型与浮点型）
                String s = n.value();
                try {
                    if (s.contains(".") || s.contains("e") || s.contains("E")) {
                        return Double.parseDouble(s);
                    }
                    return Integer.parseInt(s);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
            case StringLiteralNode s -> {
                // 处理字符串字面量
                return s.value();
            }
            case BoolLiteralNode b -> {
                // 处理布尔字面量（返回1/0便于后续IR处理）
                return b.getValue() ? 1 : 0;
            }
            case ArrayLiteralNode arr -> {
                // 递归处理数组字面量，全部元素均需可常量折叠
                List<Object> list = new ArrayList<>();
                for (ExpressionNode e : arr.elements()) {
                    Object v = tryFoldConst(e);
                    if (v == null) return null;
                    list.add(v);
                }
                return List.copyOf(list);
            }
            case IdentifierNode id -> {
                // 处理常量变量（仅支持当前作用域中可直接取值的常量）
                try {
                    Object v = ctx.ctx().getScope().getConstValue(id.name());
                    if (v != null) return v;
                } catch (Throwable ignored) {
                    // 忽略所有异常，保持鲁棒性
                }
            }
            default -> {
                // 其它表达式（如运算、调用等）默认不支持
            }
        }

        return null;
    }
}
