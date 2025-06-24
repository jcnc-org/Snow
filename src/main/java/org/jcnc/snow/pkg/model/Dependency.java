package org.jcnc.snow.pkg.model;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * group:artifact:version 依赖坐标对象。
 * <p>
 * 支持占位符和属性映射进行动态变量替换，适用于 Snow 语言包管理和源码依赖场景。
 * </p>
 *
 * <pre>
 * 示例用法：
 * Dependency dep = Dependency.fromString(
 *     "core", "com.example:core:@{version}",
 *     Map.of("version", "1.2.3")
 * );
 * </pre>
 *
 * @param id       依赖唯一标识
 * @param group    组织/分组名
 * @param artifact 构件名
 * @param version  版本号
 */
public record Dependency(
        String id,
        String group,
        String artifact,
        String version
) {

    /** 匹配 group:artifact:version 格式的正则表达式。 */
    private static final Pattern GAV = Pattern.compile("([^:]+):([^:]+):(.+)");

    /**
     * 根据字符串坐标和属性映射创建依赖对象。
     * <p>
     * 坐标中的占位符（如 <code>@{key}</code>）会用 props 中对应的值替换。
     * </p>
     *
     * @param id         依赖唯一标识
     * @param coordinate 坐标字符串，格式为 group:artifact:version，支持占位符
     * @param props      占位符替换属性映射
     * @return 解析后的 Dependency 实例
     * @throws IllegalArgumentException 坐标格式非法时抛出
     */
    public static Dependency fromString(String id, String coordinate, Map<String, String> props) {
        // 替换占位符
        String resolved = coordinate;
        for (Map.Entry<String, String> p : props.entrySet()) {
            resolved = resolved.replace("@{" + p.getKey() + "}", p.getValue());
        }

        Matcher m = GAV.matcher(resolved);
        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid dependency format: " + coordinate);
        }
        return new Dependency(id, m.group(1), m.group(2), m.group(3));
    }

    /**
     * 生成依赖对应的源码文件路径。
     * <p>
     * 路径格式：groupId/artifactId/version/artifactId.snow
     * 例如：com/example/core/1.2.3/core.snow
     * </p>
     *
     * @return 仓库源码文件的相对路径
     */
    public String toPath() {
        String groupPath = group.replace('.', '/');
        return groupPath + "/" + artifact + "/" + version + "/" + artifact + ".snow";
    }

    /**
     * 返回该依赖的 group:artifact:version 字符串表示。
     *
     * @return 坐标字符串
     */
    @Override
    public String toString() {
        return group + ":" + artifact + ":" + version;
    }
}
