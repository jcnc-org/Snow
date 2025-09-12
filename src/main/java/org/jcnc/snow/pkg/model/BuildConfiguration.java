package org.jcnc.snow.pkg.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 构建配置对象，封装构建过程中的所有选项。
 * <p>
 * 支持模板变量（形如 <code>@{key}</code>）的值自动替换。
 * </p>
 */
public final class BuildConfiguration {

    /**
     * 存储所有配置项
     */
    private final Map<String, String> options;

    /**
     * 私有构造函数，仅供工厂方法调用。
     *
     * @param options 配置项键值对
     */
    private BuildConfiguration(Map<String, String> options) {
        this.options = options;
    }

    /**
     * 基于原始配置项和属性集创建配置对象。
     * <ul>
     *   <li>会将所有值中的 <code>@{key}</code> 模板变量，替换为 props 中对应的值</li>
     *   <li>属性未匹配到时保留原模板</li>
     * </ul>
     *
     * @param flat  原始配置项，值中可包含模板变量
     * @param props 变量替换用的属性集
     * @return 处理后生成的配置对象
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
     * 获取指定配置项的值。
     *
     * @param key 配置项名称
     * @param def 默认值（未找到时返回）
     * @return 配置项值，若不存在则返回默认值
     */
    public String get(String key, String def) {
        return options.getOrDefault(key, def);
    }

    @Override
    public String toString() {
        return options.toString();
    }
}
