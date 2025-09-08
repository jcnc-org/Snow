package org.jcnc.snow.compiler.semantic.type;

import java.util.List;
import java.util.Objects;

/**
 * {@code FunctionType} 表示函数的类型信息，由<strong>参数类型列表</strong>和<strong>返回类型</strong>组成。
 * <p>
 * 适用于函数声明、函数调用、类型检查等语义分析场景。
 * <p>
 * 例如，一个函数接受两个 {@code int} 参数并返回 {@code string}，其函数类型为:
 * <pre>
 *   (int, int) -> string
 * </pre>
 *
 * <p>该类使用 Java 16+ {@code record} 语法定义，自动提供:
 * <ul>
 *   <li>构造方法；</li>
 *   <li>访问器 {@code paramTypes()} 和 {@code returnType()}；</li>
 *   <li>{@code equals()}, {@code hashCode()}, {@code toString()} 方法；</li>
 * </ul>
 *
 * <p>实现接口: {@link Type}
 *
 * @param paramTypes 参数类型列表（顺序敏感，不可为 null）
 * @param returnType 返回类型（不可为 null）
 */
public record FunctionType(List<Type> paramTypes, Type returnType) implements Type {

    /**
     * 构造函数类型对象。
     * <p>
     * 参数类型列表将被包装为不可变，以确保函数类型不可修改。
     *
     * @param paramTypes 参数类型列表
     * @param returnType 返回类型
     */
    public FunctionType(List<Type> paramTypes, Type returnType) {
        this.paramTypes = List.copyOf(paramTypes); // 确保不可变
        this.returnType = returnType;
    }

    /**
     * 判断当前函数类型是否与另一个类型兼容。
     * <p>
     * 兼容条件:
     * <ul>
     *   <li>对方也是 {@code FunctionType}；</li>
     *   <li>返回类型兼容（可宽化或完全匹配）；</li>
     *   <li>参数列表完全相等（类型与顺序严格一致）。</li>
     * </ul>
     *
     * @param other 要检查的另一个类型
     * @return 若兼容返回 {@code true}，否则 {@code false}
     */
    @Override
    public boolean isCompatible(Type other) {
        if (!(other instanceof FunctionType(List<Type> types, Type type))) return false;
        return returnType.isCompatible(type) && paramTypes.equals(types);
    }

    /**
     * 返回函数类型的标准字符串表示形式。
     * <p>
     * 格式为:
     * <pre>
     *   (param1, param2, ...) -> returnType
     * </pre>
     * 例如 {@code (int, string) -> void}
     *
     * @return 函数类型的可读性描述
     */
    @Override
    public String toString() {
        return "(" + paramTypes + ") -> " + returnType;
    }

    /**
     * 判断两个函数类型是否相等。
     * <p>
     * 相等条件为:
     * <ul>
     *   <li>引用相同；</li>
     *   <li>或参数类型列表完全相等，且返回类型也相等。</li>
     * </ul>
     *
     * @param obj 待比较的对象
     * @return 若相等返回 {@code true}，否则 {@code false}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof FunctionType(List<Type> types, Type type))) return false;
        return returnType.equals(type) && paramTypes.equals(types);
    }

    /**
     * 计算哈希码，确保与 {@link #equals(Object)} 保持一致性。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(paramTypes, returnType);
    }
}
