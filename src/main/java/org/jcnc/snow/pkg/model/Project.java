package org.jcnc.snow.pkg.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 表示一个软件包/模块的项目信息，包括元数据、属性、仓库、依赖和构建配置等。
 * <p>
 * 本类为不可变对象，仅提供getter，无setter。
 * 支持通过 {@link #fromFlatMap(Map)} 静态工厂方法从扁平Map快速创建。
 * </p>
 *
 * <pre>
 * Map&lt;String,String&gt; map = ...;
 * Project project = Project.fromFlatMap(map);
 * </pre>
 */
public final class Project {

    /** 组织/分组名（如com.example） */
    private final String group;
    /** 构件/模块名（如app-core） */
    private final String artifact;
    /** 项目展示名称 */
    private final String name;
    /** 版本号（如1.0.0） */
    private final String version;
    /** 项目描述 */
    private final String description;
    /** 许可证标识 */
    private final String license;
    /** 项目主页URL */
    private final String homepage;

    /** 额外属性（不影响主字段，可用于模板/占位符） */
    private final Map<String, String> properties;
    /** 仓库列表（仓库ID -> 仓库对象） */
    private final Map<String, Repository> repositories;
    /** 依赖列表 */
    private final List<Dependency> dependencies;
    /** 构建配置 */
    private final BuildConfiguration build;


    /**
     * 构造函数（私有），请使用 {@link #fromFlatMap(Map)} 创建实例。
     */
    private Project(
            String group,
            String artifact,
            String name,
            String version,
            String description,
            String license,
            String homepage,
            Map<String, String> properties,
            Map<String, Repository> repositories,
            List<Dependency> dependencies,
            BuildConfiguration build
    ) {
        this.group = group;
        this.artifact = artifact;
        this.name = name;
        this.version = version;
        this.description = description;
        this.license = license;
        this.homepage = homepage;
        this.properties = properties;
        this.repositories = repositories;
        this.dependencies = dependencies;
        this.build = build;
    }

    /**
     * 通过扁平Map创建 Project 实例。约定key格式如下：
     * <ul>
     *   <li>project.*      —— 项目元数据</li>
     *   <li>properties.*   —— 额外属性</li>
     *   <li>repositories.* —— 仓库</li>
     *   <li>dependencies.* —— 依赖</li>
     *   <li>build.*        —— 构建配置</li>
     * </ul>
     *
     * @param map 扁平的配置map
     * @return Project 实例
     */
    public static Project fromFlatMap(Map<String, String> map) {

        // 1. simple project metadata
        String group = map.getOrDefault("project.group", "unknown");
        String artifact = map.getOrDefault("project.artifact", "unknown");
        String name = map.getOrDefault("project.name", artifact);
        String version = map.getOrDefault("project.version", "0.0.1-SNAPSHOT");
        String description = map.getOrDefault("project.description", "");
        String license = map.getOrDefault("project.license", "");
        String homepage = map.getOrDefault("project.homepage", "");

        // 2. properties.*
        Map<String, String> props = new LinkedHashMap<>();
        map.forEach((k, v) -> {
            if (k.startsWith("properties.")) {
                props.put(k.substring("properties.".length()), v);
            }
        });

        // 3. repositories.*
        Map<String, Repository> repos = new LinkedHashMap<>();
        map.forEach((k, v) -> {
            if (k.startsWith("repositories.")) {
                String id = k.substring("repositories.".length());
                repos.put(id, new Repository(id, v));
            }
        });

        // 4. dependencies.*
        List<Dependency> deps = new ArrayList<>();
        map.forEach((k, v) -> {
            if (k.startsWith("dependencies.")) {
                String id = k.substring("dependencies.".length());
                deps.add(Dependency.fromString(id, v, props));
            }
        });

        // 5. build.* simply hand the subtree map
        Map<String, String> buildMap = new LinkedHashMap<>();
        map.forEach((k, v) -> {
            if (k.startsWith("build.")) {
                buildMap.put(k.substring("build.".length()), v);
            }
        });

        BuildConfiguration buildCfg = BuildConfiguration.fromFlatMap(buildMap, props);

        return new Project(group, artifact, name, version, description, license, homepage, props, repos, deps, buildCfg);
    }

    /** @return 组织/分组名 */
    public String getGroup() {
        return group;
    }

    /** @return 构件/模块名 */
    public String getArtifact() {
        return artifact;
    }

    /** @return 项目名称 */
    public String getName() {
        return name;
    }

    /** @return 版本号 */
    public String getVersion() {
        return version;
    }

    /** @return 项目描述 */
    public String getDescription() {
        return description;
    }

    /** @return 许可证 */
    public String getLicense() {
        return license;
    }

    /** @return 项目主页URL */
    public String getHomepage() {
        return homepage;
    }

    /** @return 额外属性映射 */
    public Map<String, String> getProperties() {
        return properties;
    }

    /** @return 仓库映射 */
    public Map<String, Repository> getRepositories() {
        return repositories;
    }

    /** @return 依赖列表 */
    public List<Dependency> getDependencies() {
        return dependencies;
    }

    /** @return 构建配置 */
    public BuildConfiguration getBuild() {
        return build;
    }
}
