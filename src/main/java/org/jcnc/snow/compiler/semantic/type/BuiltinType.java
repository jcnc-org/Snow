package org.jcnc.snow.compiler.semantic.type;

/**
 * {@code BuiltinType} 枚举定义了本语言支持的所有内置基础类型。
 * <p>
 * 类型涵盖整数、浮点、字符串、void 以及万能 {@code any} 类型，
 * 广泛应用于变量声明、表达式类型推导、函数签名、类型检查等语义分析环节。
 * <p>
 * 支持的类型包括：
 * <ul>
 *   <li>{@link #BYTE}    - 8 位整数</li>
 *   <li>{@link #SHORT}   - 16 位整数</li>
 *   <li>{@link #INT}     - 32 位整数</li>
 *   <li>{@link #LONG}    - 64 位整数</li>
 *   <li>{@link #FLOAT}   - 单精度浮点数</li>
 *   <li>{@link #DOUBLE}  - 双精度浮点数</li>
 *   <li>{@link #STRING}  - 字符串</li>
 *   <li>{@link #BOOLEAN} - 布尔</li>
 *   <li>{@link #VOID}    - 空类型，用于表示无返回值</li>
 *   <li>{@link #ANY}     - 万能类型，可与任意基础类型兼容</li>
 * </ul>
 */
public enum BuiltinType implements Type {

    /**
     * 8 位整数类型。
     */
    BYTE,

    /**
     * 16 位整数类型。
     */
    SHORT,

    /**
     * 32 位整数类型（默认整型）。
     */
    INT,

    /**
     * 64 位整数类型。
     */
    LONG,

    /**
     * 单精度浮点数。
     */
    FLOAT,

    /**
     * 双精度浮点数。
     */
    DOUBLE,

    /**
     * 字符串类型。
     */
    STRING,

    /**
     * 布尔类型。
     */
    BOOLEAN,

    /**
     * 空类型（无返回值）。
     */
    VOID,

    /**
     * 万能类型，可与任意基础类型兼容。
     */
    ANY;


    /**
     * 判断当前类型与另一个类型是否兼容。
     * <ul>
     *   <li>任意一方为 {@code ANY} 时兼容</li>
     *   <li>类型完全一致时兼容</li>
     *   <li>数值类型之间支持自动宽化转换（如 int -> long, float -> double）</li>
     * </ul>
     *
     * @param other 另一个类型
     * @return 如果兼容返回 true，否则返回 false
     */
    @Override
    public boolean isCompatible(Type other) {
        if (this == ANY || other == ANY) return true;
        if (this == other) return true;
        if (this.isNumeric() && other.isNumeric()) {
            return Type.widen(other, this) == this;
        }
        return false;
    }

    /**
     * 判断当前类型是否为数值类型（byte/short/int/long/float/double）。
     * <ul>
     *   <li>数值类型包括 BYTE、SHORT、INT、LONG、FLOAT、DOUBLE</li>
     * </ul>
     *
     * @return 如果为数值类型返回 true，否则返回 false
     */
    @Override
    public boolean isNumeric() {
        return switch (this) {
            case BYTE, SHORT, INT, LONG, FLOAT, DOUBLE -> true;
            default -> false;
        };
    }

    /**
     * 获取类型的小写名称，常用于日志输出与错误提示。
     *
     * @return 当前类型名的小写字符串
     */
    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
