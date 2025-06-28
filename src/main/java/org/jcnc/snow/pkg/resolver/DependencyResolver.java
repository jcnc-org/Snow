package org.jcnc.snow.pkg.resolver;

import org.jcnc.snow.pkg.model.Dependency;
import org.jcnc.snow.pkg.model.Project;
import org.jcnc.snow.pkg.model.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * 负责解析并下载项目依赖的工具类。
 * <p>
 * 支持本地缓存，如果依赖已存在于本地缓存则直接使用，否则尝试从项目仓库下载依赖包。
 * </p>
 */
public final class DependencyResolver {

    /** 本地缓存目录 */
    private final Path localCache;

    /**
     * 创建 DependencyResolver 实例。
     *
     * @param localCacheDir 用于缓存依赖的本地目录
     */
    public DependencyResolver(Path localCacheDir) {
        this.localCache = localCacheDir;
    }

    /**
     * 解析并下载指定项目的所有依赖。
     * <p>
     * 依赖优先从本地缓存读取，若未命中，则尝试从第一个配置的仓库下载。
     * </p>
     *
     * @param project 要解析依赖的项目
     * @throws IOException 下载或文件操作失败时抛出
     */
    public void resolve(Project project) throws IOException, URISyntaxException {
        Files.createDirectories(localCache);
        for (Dependency dep : project.getDependencies()) {
            Path jarPath = localCache.resolve(dep.toPath());
            if (Files.exists(jarPath)) {
                System.out.println("[dependency] " + dep + " resolved from cache.");
                continue;
            }

            // 从第一个仓库下载
            Optional<Repository> repo = project.getRepositories().values().stream().findFirst();
            if (repo.isEmpty()) {
                throw new IOException("No repository configured for dependency " + dep);
            }

            String url = repo.get().url() + "/" + dep.toPath();
            download(url, jarPath);
        }
    }

    /**
     * 从指定 URL 下载文件到本地目标路径。
     *
     * @param urlStr 远程文件 URL
     * @param dest   本地目标路径
     * @throws IOException 下载或保存文件时出错
     */
    private void download(String urlStr, Path dest) throws IOException, URISyntaxException {
        System.out.println("[download] " + urlStr);
        Files.createDirectories(dest.getParent());
        URL url = new URI(urlStr).toURL();
        try (InputStream in = url.openStream()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }
        System.out.println("[saved] " + dest);
    }
}
