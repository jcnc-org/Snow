package org.jcnc.snow.compiler.semantic.type;

import java.util.Objects;

/**
 * {@code ArrayType} 表示数组类型，每个数组类型包含其元素类型。
 * <p>
 * 例如，int[]、string[] 等均可用本类表示。内部通过 {@link #elementType()} 字段保存元素类型。
 * </p>
 */
public record ArrayType(
        /*
          数组元素的类型。
         */
        Type elementType
) implements Type {

    /**
     * 判断当前数组类型能否与另一类型兼容（主要用于类型检查）。
     * <p>
     * 只有当 {@code other} 也是 ArrayType，且元素类型兼容时返回 true。
     * </p>
     *
     * @param other 需判断的类型
     * @return 类型兼容性结果
     */
    @Override
    public boolean isCompatible(Type other) {
        if (!(other instanceof ArrayType(Type type))) return false;
        return elementType.isCompatible(type);
    }

    /**
     * 数组类型不是数值类型，直接调用父接口默认实现。
     *
     * @return 总为 false
     */
    @Override
    public boolean isNumeric() {
        return Type.super.isNumeric();
    }

    /**
     * 判断两个 ArrayType 是否等价（元素类型完全一致即视为等价）。
     *
     * @param o 比较对象
     * @return 是否等价
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof ArrayType(Type type) && Objects.equals(elementType, type);
    }

    /**
     * 返回数组类型的字符串描述，如 "int[]"。
     *
     * @return 类型名称
     */
    @Override
    public String toString() {
        return name();
    }

    /**
     * 获取数组类型的名称描述，如 "int[]"。
     *
     * @return 类型名称
     */
    @Override
    public String name() {
        return elementType.name() + "[]";
    }
}
