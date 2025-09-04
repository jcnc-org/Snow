package org.jcnc.snow.compiler.ir.builder.utils;

import org.jcnc.snow.compiler.ir.builder.core.IRContext;
import org.jcnc.snow.compiler.parser.ast.*;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 常量折叠工具类。
 * <p>
 * 尝试在编译期将表达式计算为具体常量值，
 * 用于编译器优化和提前求值。
 * 支持数字、字符串、布尔、数组、简单标识符等字面量及常量变量。
 *
 * @param ctx 编译期上下文环境，用于常量查找等
 */
public record TryFoldConst(IRContext ctx) {

    /**
     * 构造方法，注入上下文。
     *
     * @param ctx IR 编译上下文
     */
    public TryFoldConst {
    }

    /**
     * 尝试将表达式节点折叠为常量 Java 值。
     *
     * @param expr 任意表达式节点
     * @return 若可折叠则返回 Java 值（如 Integer、Double、String、List 等），否则返回 null
     */
    public Object apply(ExpressionNode expr) {
        if (expr == null) return null;

        switch (expr) {
            // 数字字面量处理：支持整数和浮点数
            case NumberLiteralNode n -> {
                String s = n.value();
                try {
                    if (s.contains(".") || s.contains("e") || s.contains("E")) {
                        return Double.parseDouble(s);
                    }
                    return Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    return null; // 非法数字返回 null
                }
            }
            // 字符串字面量
            case StringLiteralNode s -> {
                return s.value();
            }
            // 布尔字面量，true 映射为 1，false 映射为 0
            case BoolLiteralNode b -> {
                return b.getValue() ? 1 : 0;
            }
            // 数组字面量，递归折叠所有元素，任一元素失败则整体失败
            case ArrayLiteralNode arr -> {
                List<Object> list = new ArrayList<>();
                for (ExpressionNode e : arr.elements()) {
                    Object v = apply(e);
                    if (v == null) return null; // 有一个不能折叠就直接返回 null
                    list.add(v);
                }
                return List.copyOf(list); // 返回不可变数组
            }
            // 标识符（变量），尝试查找上下文的常量值
            case IdentifierNode id -> {
                try {
                    return ctx.getScope().getConstValue(id.name());
                } catch (Throwable ignored) {
                    return null;
                }
            }
            // 其它类型默认不支持折叠
            default -> {
                return null;
            }
        }
    }
}
