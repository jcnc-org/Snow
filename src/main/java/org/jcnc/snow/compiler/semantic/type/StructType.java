package org.jcnc.snow.compiler.semantic.type;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * {@code StructType}
 * <p>
 * 表示用户自定义结构体类型，在类型系统中用于描述结构体实例的静态类型信息。
 * <ul>
 *   <li>由 <b>(moduleName, structName)</b> 唯一标识（支持跨模块同名 struct）。</li>
 *   <li>包含字段定义、方法签名、构造函数等全部类型元信息。</li>
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
     * 字段定义表：字段名 -> 字段类型
     */
    private final Map<String, Type> fields = new HashMap<>();
    /**
     * 方法签名表：方法名 -> 函数类型
     */
    private final Map<String, FunctionType> methods = new HashMap<>();
    /**
     * 构造函数（init）的函数类型；可为 null（表示无参默认构造）
     */
    private FunctionType constructor;

    /**
     * 构造函数：创建结构体类型描述。
     *
     * @param moduleName 所属模块名（不可为空）
     * @param name       结构体名称（不可为空）
     */
    public StructType(String moduleName, String name) {
        this.moduleName = moduleName;
        this.name = name;
    }

    /**
     * 获取结构体所属模块名
     */
    public String moduleName() {
        return moduleName;
    }

    /**
     * 获取结构体名称
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * 获取构造函数签名（可为 null，表示默认无参构造）
     */
    public FunctionType getConstructor() {
        return constructor;
    }

    /**
     * 设置构造函数签名
     */
    public void setConstructor(FunctionType ctor) {
        this.constructor = ctor;
    }

    /**
     * 获取所有字段定义（字段名 → 字段类型）
     */
    public Map<String, Type> getFields() {
        return fields;
    }

    /**
     * 获取所有方法签名（方法名 → 函数类型）
     */
    public Map<String, FunctionType> getMethods() {
        return methods;
    }

    /**
     * 判断类型兼容性：
     * <ul>
     *   <li>仅模块名与结构体名都相等才视为兼容。</li>
     *   <li>跨模块同名 struct 不兼容。</li>
     * </ul>
     *
     * @param other 另一个类型
     * @return 类型兼容返回 true，否则 false
     */
    @Override
    public boolean isCompatible(Type other) {
        if (!(other instanceof StructType s)) return false;
        return Objects.equals(this.name, s.name)
                && Objects.equals(this.moduleName, s.moduleName);
    }

    /**
     * 判断类型相等：模块名+结构体名全等。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StructType s)) return false;
        return Objects.equals(name, s.name) && Objects.equals(moduleName, s.moduleName);
    }

    /**
     * 哈希值定义，和 equals 保持一致（用于 Map/Set 索引）
     */
    @Override
    public int hashCode() {
        return Objects.hash(moduleName, name);
    }

    /**
     * 字符串表示：返回结构体名（调试用）
     */
    @Override
    public String toString() {
        return name;
    }
}
