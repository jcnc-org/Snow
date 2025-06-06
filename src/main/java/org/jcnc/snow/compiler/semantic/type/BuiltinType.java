package org.jcnc.snow.compiler.semantic.type;

/**
 * {@code BuiltinType} 枚举定义了本语言支持的所有内置基础类型。
 * <p>
 * 类型涵盖整数、浮点、字符串及 void 类型，广泛应用于变量声明、
 * 表达式类型推导、函数签名、类型检查等语义分析环节。
 *
 * <p>支持的类型包括：
 * <ul>
 *   <li>{@link #BYTE}   - 8 位整数</li>
 *   <li>{@link #SHORT}  - 16 位整数</li>
 *   <li>{@link #INT}    - 32 位整数</li>
 *   <li>{@link #LONG}   - 64 位整数</li>
 *   <li>{@link #FLOAT}  - 单精度浮点数</li>
 *   <li>{@link #DOUBLE} - 双精度浮点数</li>
 *   <li>{@link #STRING} - 字符串类型</li>
 *   <li>{@link #VOID}   - 空类型，用于表示无返回值的函数</li>
 * </ul>
 *
 * <p>每个枚举实例实现了 {@link Type} 接口，提供以下语义特性：
 * <ul>
 *   <li>数值类型判断 {@link #isNumeric()}；</li>
 *   <li>类型兼容性判断 {@link #isCompatible(Type)}；</li>
 *   <li>自动数值宽化支持（通过 {@link Type#widen(Type, Type)} 实现）。</li>
 * </ul>
 */
public enum BuiltinType implements Type {

    BYTE,    // 8 位有符号整数
    SHORT,   // 16 位有符号整数
    INT,     // 32 位有符号整数（默认整数类型）
    LONG,    // 64 位有符号整数
    FLOAT,   // 单精度浮点数
    DOUBLE,  // 双精度浮点数
    STRING,  // 字符串类型
    VOID;    // 空类型，用于表示函数无返回值

    /**
     * 判断当前类型是否与指定类型兼容。
     * <p>
     * 兼容判断规则：
     * <ul>
     *   <li>类型完全相同，视为兼容；</li>
     *   <li>对于数值类型，若目标类型为宽类型（如 int → double），视为兼容；</li>
     *   <li>其他情况视为不兼容。</li>
     * </ul>
     *
     * @param other 要比较的类型
     * @return 若兼容返回 {@code true}，否则返回 {@code false}
     */
    @Override
    public boolean isCompatible(Type other) {
        if (this == other) return true;
        // 数值类型间允许自动宽化
        if (this.isNumeric() && other.isNumeric()) {
            return Type.widen(other, this) == this;
        }
        return false;
    }

    /**
     * 判断当前类型是否为数值类型。
     * <p>
     * 数值类型包括：
     * {@link #BYTE}、{@link #SHORT}、{@link #INT}、{@link #LONG}、
     * {@link #FLOAT}、{@link #DOUBLE}。
     *
     * @return 若为数值类型返回 {@code true}，否则返回 {@code false}
     */
    @Override
    public boolean isNumeric() {
        return switch (this) {
            case BYTE, SHORT, INT, LONG, FLOAT, DOUBLE -> true;
            default -> false;
        };
    }

    /**
     * 获取当前类型的名称（小写形式）。
     * <p>
     * 用于日志输出、错误提示等语义描述场景。
     *
     * @return 当前类型的名称字符串（如 "int", "string", "void" 等）
     */
    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
