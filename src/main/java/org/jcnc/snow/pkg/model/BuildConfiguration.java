package org.jcnc.snow.pkg.model;

import java.util.Map;
import java.util.HashMap;

/**
 * 构建配置对象，封装构建过程中的所有选项。
 * <p>
 * 支持基于模板变量（形如{@code @{key}}）的选项值替换。
 * </p>
 */
public final class BuildConfiguration {

    /** 存储配置项的键值对 */
    private final Map<String, String> options;

    /**
     * 私有构造函数，用于初始化配置项。
     *
     * @param options 配置项键值对
     */
    private BuildConfiguration(Map<String, String> options) {
        this.options = options;
    }

    /**
     * 基于原始配置项和变量属性创建配置对象。
     * <p>
     * 会将原始配置中的所有值中的{@code @{key}}模板，替换为属性props中对应的值。
     * </p>
     *
     * @param flat  原始的配置项，值中可包含模板变量（如@{name}）
     * @param props 用于替换模板变量的属性集
     * @return 构建完成的配置对象
     */
    public static BuildConfiguration fromFlatMap(Map<String, String> flat, Map<String, String> props) {
        Map<String, String> resolved = new HashMap<>();
        for (Map.Entry<String, String> e : flat.entrySet()) {
            String value = e.getValue();
            for (Map.Entry<String, String> p : props.entrySet()) {
                value = value.replace("@{" + p.getKey() + "}", p.getValue());
            }
            resolved.put(e.getKey(), value);
        }
        return new BuildConfiguration(resolved);
    }

    /**
     * 获取指定key对应的配置值。
     *
     * @param key 配置项名称
     * @param def 默认值（若未找到key则返回此值）
     * @return 配置项对应值，若不存在则返回默认值
     */
    public String get(String key, String def) {
        return options.getOrDefault(key, def);
    }

    @Override
    public String toString() {
        return options.toString();
    }
}
