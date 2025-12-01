package org.jcnc.snow.compiler.ir.builder.utils;

import org.jcnc.snow.compiler.common.NumberLiteralHelper;
import org.jcnc.snow.compiler.ir.builder.core.IRContext;
import org.jcnc.snow.compiler.parser.ast.*;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 编译期常量折叠工具类。
 *
 * <p>
 * 用于在 IR 构造阶段尝试将表达式提前计算为静态常量，以支持：
 * </p>
 * <ul>
 *     <li>常量传播（constant propagation）；</li>
 *     <li>编译期常量折叠（constant folding）；</li>
 *     <li>提升运行期性能，减少无意义计算；</li>
 *     <li>提前评估数组、字面量、常量变量等表达式。</li>
 * </ul>
 *
 * <p>
 * 该类支持以下表达式的编译期求值：
 * </p>
 * <ul>
 *     <li>数字字面量（含十进制与十六进制）；</li>
 *     <li>字符串字面量；</li>
 *     <li>布尔字面量（以 1/0 表示）；</li>
 *     <li>数组字面量（递归折叠所有元素）；</li>
 *     <li>标识符（若查到上下文中的常量值）；</li>
 * </ul>
 *
 * <p>
 * 若表达式无法被折叠，则返回 {@code null}。
 * </p>
 */
public record TryFoldConst(IRContext ctx) {

    /**
     * 构造函数。
     *
     * @param ctx IR 编译上下文，用于访问作用域常量等信息
     */
    public TryFoldConst {
    }

    /**
     * 尝试将表达式折叠为运行期 Java 对象。
     *
     * <p>
     * 若折叠成功，返回一个代表常量的 Java 值，例如：
     * </p>
     * <ul>
     *     <li>{@code Integer} / {@code Double} 数字类型；</li>
     *     <li>{@code String} 字符串类型；</li>
     *     <li>{@code Integer}（布尔用 1/0 表示）；</li>
     *     <li>{@code List<Object>} 不可变列表（数组字面量）；</li>
     * </ul>
     *
     * <p>
     * 若表达式无法折叠（如存在变量、调用、复杂副作用等）则返回 {@code null}。
     * </p>
     *
     * @param expr 任意表达式节点
     * @return 可折叠为常量时返回 Java 值，否则返回 null
     */
    public Object apply(ExpressionNode expr) {
        if (expr == null) return null;

        switch (expr) {

            // 数字字面量
            case NumberLiteralNode n -> {
                String s = n.value();
                try {
                    var normalized = NumberLiteralHelper.normalize(s, true);

                    // 浮点数 → double
                    if (NumberLiteralHelper.looksLikeFloat(normalized.text())) {
                        return Double.parseDouble(normalized.text());
                    }

                    // 整数 → int（可自动处理十六进制）
                    return NumberLiteralHelper.parseIntLiteral(normalized.digits(), normalized.radix());
                } catch (NumberFormatException e) {
                    return null; // 非法字面量
                }
            }

            // 字符串字面量
            case StringLiteralNode s -> {
                return s.value();
            }

            // 布尔字面量（语义上为 int：true=1 / false=0）
            case BoolLiteralNode b -> {
                return b.getValue() ? 1 : 0;
            }

            // 数组字面量：递归折叠所有元素
            case ArrayLiteralNode arr -> {
                List<Object> list = new ArrayList<>();
                for (ExpressionNode e : arr.elements()) {
                    Object v = apply(e);
                    if (v == null) return null; // 任一元素无法折叠 → 整体失败
                    list.add(v);
                }
                return List.copyOf(list); // 返回不可变列表
            }

            // 标识符：尝试从上下文查找常量值
            case IdentifierNode id -> {
                try {
                    return ctx.getScope().getConstValue(id.name());
                } catch (Throwable ignored) {
                    return null;
                }
            }

            // 不支持折叠的表达式
            default -> {
                return null;
            }
        }
    }
}