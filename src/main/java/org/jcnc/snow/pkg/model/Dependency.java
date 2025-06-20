package org.jcnc.snow.pkg.model;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 表示一个 Maven 风格的 group:artifact:version 依赖坐标。
 * <p>
 * 支持通过占位符和属性映射进行动态变量替换，可用于构建工具或依赖管理场景。
 * </p>
 *
 * <pre>
 * 示例：
 * Dependency dep = Dependency.fromString(
 *     "core", "com.example:core:@{version}",
 *     Map.of("version", "1.2.3")
 * );
 * </pre>
 */
public record Dependency(
        String id,
        String group,
        String artifact,
        String version
) {

    /**
     * 用于匹配 group:artifact:version 格式的正则表达式。
     */
    private static final Pattern GAV = Pattern.compile("([^:]+):([^:]+):(.+)");

    /**
     * 根据字符串坐标和属性映射创建依赖对象。
     * <p>
     * 坐标中的占位符如 {@code @{key}} 会用 props 中对应的值替换。
     * </p>
     *
     * @param id         依赖唯一标识
     * @param coordinate 依赖坐标字符串，格式为 group:artifact:version，支持变量占位符
     * @param props      占位符替换的属性映射
     * @return 解析后的 Dependency 实例
     * @throws IllegalArgumentException 如果坐标格式非法
     */
    public static Dependency fromString(String id, String coordinate, Map<String, String> props) {
        // 替换 @{prop} 占位符
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
     * 生成依赖对应的标准仓库 jar 路径。
     * <p>
     * 路径格式通常为：groupId/artifactId/version/artifactId-version.jar<br>
     * 例如：com/example/core/1.2.3/core-1.2.3.jar
     * </p>
     *
     * @return 仓库 jar 文件的相对路径
     */
    public String toPath() {
        String groupPath = group.replace('.', '/');
        return groupPath + "/" + artifact + "/" + version + "/" + artifact + "-" + version + ".jar";
    }

    /**
     * 返回该依赖的 group:artifact:version 字符串表示。
     *
     * @return Maven 坐标字符串
     */
    @Override
    public String toString() {
        return group + ":" + artifact + ":" + version;
    }
}
