package org.jcnc.snow.pkg.model;

/**
 * 表示一个远程仓库的基本信息，通常用于依赖解析和发布。
 * <p>
 * 每个仓库由唯一的 ID 和对应的 URL 标识。
 * </p>
 *
 * <pre>
 * 示例用法：
 * Repository repo = new Repository("central", "https://");
 * </pre>
 *
 * @param id  仓库唯一标识
 * @param url 仓库地址（通常为 HTTP(S) 链接）
 */
public record Repository(String id, String url) {
}
