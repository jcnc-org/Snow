package org.jcnc.snow.compiler.semantic.type;

/**
 * 类型接口: 所有类型（包括内置类型、函数类型等）均需实现此接口，
 * 用于在语义分析中进行类型兼容性检查和统一表示。
 */
public interface Type {
    /**
     * 判断当前类型是否与另一个类型兼容。
     *
     * @param other 要检查兼容性的另一个类型
     * @return 如果兼容则返回 true，否则返回 false
     */
    boolean isCompatible(Type other);

    /**
     * 判断当前类型是否为数值类型（byte/short/int/long/float/double）。
     * <p>
     * 默认实现返回 false，BuiltinType 会覆盖此方法。
     *
     * @return 如果是数值类型则返回 true，否则返回 false
     */
    default boolean isNumeric() {
        return false;
    }

    /**
     * 对两个数值类型执行宽化转换，返回“更宽”的那个类型。
     * <p>
     * 若 a 和 b 都是数值类型，则按 byte→short→int→long→float→double 顺序选更宽的类型；
     * 否则返回 null。
     *
     * @param a 第一个类型
     * @param b 第二个类型
     * @return 两者中更宽的数值类型，或 null
     */
    static Type widen(Type a, Type b) {
        if (!(a.isNumeric() && b.isNumeric())) return null;
        var order = java.util.List.of(
                BuiltinType.BYTE,
                BuiltinType.SHORT,
                BuiltinType.INT,
                BuiltinType.LONG,
                BuiltinType.FLOAT,
                BuiltinType.DOUBLE
        );
        int ia = order.indexOf(a), ib = order.indexOf(b);
        return order.get(Math.max(ia, ib));
    }
}
