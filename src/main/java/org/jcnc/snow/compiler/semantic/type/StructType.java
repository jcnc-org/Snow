package org.jcnc.snow.compiler.semantic.type;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * {@code StructType}
 * <p>
 * 表示用户自定义结构体类型，用于类型系统中描述结构体实例的静态类型信息。
 * <ul>
 *   <li>由 <b>(moduleName, structName)</b> 唯一标识（支持跨模块同名 struct）。</li>
 *   <li>包含字段定义、方法签名、构造函数等全部类型元信息。</li>
 *   <li>支持父类引用，用于实现继承与运行时多态。</li>
 * </ul>
 * </p>
 */
public class StructType implements Type {
    /**
     * 所属模块名称，用于唯一标识、支持跨模块同名结构体
     */
    private final String moduleName;

    /**
     * 结构体类型名称（如 "Person"）
     */
    private final String name;

    /**
     * 构造函数签名表：参数个数 → 构造函数类型
     */
    private final Map<Integer, FunctionType> constructors = new HashMap<>();

    /**
     * 字段定义表：字段名 → 字段类型
     */
    private final Map<String, Type> fields = new HashMap<>();

    /**
     * 方法签名表：方法名 → 函数类型
     */
    private final Map<String, FunctionType> methods = new HashMap<>();

    /**
     * 父类类型（可为 null，表示没有继承）
     */
    private StructType parent;

    /**
     * 默认构造函数（可能为 null，表示无参构造）
     */
    private FunctionType constructor;

    /**
     * 构造函数：创建结构体类型描述。
     *
     * @param moduleName 所属模块名（不可为 {@code null}）
     * @param name       结构体名称（不可为 {@code null}）
     */
    public StructType(String moduleName, String name) {
        this.moduleName = moduleName;
        this.name = name;
    }

    /**
     * 获取结构体所属模块名。
     *
     * @return 模块名称
     */
    public String moduleName() {
        return moduleName;
    }

    /**
     * 获取父类类型。
     *
     * @return 父类 {@link StructType}，若无继承则返回 {@code null}
     */
    public StructType getParent() {
        return parent;
    }

    /**
     * 设置父类类型。
     *
     * @param parent 父类 {@link StructType}
     */
    public void setParent(StructType parent) {
        this.parent = parent;
    }

    /* ---------------- 构造函数相关 ---------------- */

    /**
     * 添加一个构造函数签名。
     * <p>
     * 构造函数按参数个数唯一索引，同参数个数的构造函数将被覆盖。
     * </p>
     *
     * @param ctor 构造函数的 {@link FunctionType} 实例
     */
    public void addConstructor(FunctionType ctor) {
        constructors.put(ctor.paramTypes().size(), ctor);
    }

    /**
     * 根据参数个数获取对应构造函数。
     *
     * @param argc 参数个数
     * @return 对应的构造函数签名，若不存在则返回 {@code null}
     */
    public FunctionType getConstructor(int argc) {
        return constructors.get(argc);
    }

    /**
     * 获取所有构造函数签名（不可变视图）。
     *
     * @return 形参个数 → 构造函数签名的映射
     */
    public Map<Integer, FunctionType> getConstructors() {
        return Map.copyOf(constructors);
    }

    /**
     * 获取结构体名称。
     *
     * @return 结构体类型名
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * 获取默认构造函数。
     *
     * @return 默认构造函数类型，若不存在则返回 {@code null}
     */
    public FunctionType getConstructor() {
        return constructor;
    }

    /**
     * 设置默认构造函数。
     *
     * @param ctor 构造函数类型
     */
    public void setConstructor(FunctionType ctor) {
        this.constructor = ctor;
    }

    /**
     * 获取所有字段定义。
     *
     * @return 字段定义表：字段名 → 字段类型
     */
    public Map<String, Type> getFields() {
        return fields;
    }

    /**
     * 获取所有方法签名。
     *
     * @return 方法签名表：方法名 → 函数类型
     */
    public Map<String, FunctionType> getMethods() {
        return methods;
    }

    /* ---------------- 类型兼容性（支持继承链） ---------------- */

    /**
     * 判断类型兼容性。
     * <ul>
     *   <li>若模块名 + 结构体名相等，则兼容。</li>
     *   <li>若 {@code other} 是当前类型的子类，也兼容（支持父类引用）。</li>
     * </ul>
     *
     * @param other 另一个类型
     * @return 若兼容返回 {@code true}，否则 {@code false}
     */
    @Override
    public boolean isCompatible(Type other) {
        if (!(other instanceof StructType s)) return false;
        // 沿着继承链查找
        for (StructType cur = s; cur != null; cur = cur.parent) {
            if (this.equals(cur)) return true;
        }
        return false;
    }

    /**
     * 判断类型相等。
     * <p>
     * 比较规则：模块名 + 结构体名全等。
     * </p>
     *
     * @param o 另一个对象
     * @return 相等返回 {@code true}，否则 {@code false}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StructType s)) return false;
        return Objects.equals(name, s.name) &&
                Objects.equals(moduleName, s.moduleName);
    }

    /**
     * 计算哈希值，与 {@link #equals(Object)} 保持一致。
     * <p>
     * 用于保证在 {@link Map} / {@link java.util.Set} 中索引正确。
     * </p>
     *
     * @return 哈希码
     */
    @Override
    public int hashCode() {
        return Objects.hash(moduleName, name);
    }

    /**
     * 返回字符串表示（调试用）。
     *
     * @return 结构体类型名字符串
     */
    @Override
    public String toString() {
        return name;
    }
}
