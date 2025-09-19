package org.jcnc.snow.pkg.tasks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * CleanTask 负责自动化构建流程中的清理任务，实现对构建输出目录（如 {@code build} 和 {@code dist}）的递归删除。
 * <p>
 * 常用于项目自动化构建、发布前清理环境等场景。
 * <ul>
 *   <li>线程安全，无状态实现</li>
 *   <li>实现 {@link Task} 接口，可直接在任务队列或 CLI 中调用</li>
 * </ul>
 * <b>示例用法：</b>
 * <pre>{@code
 * Task clean = new CleanTask();
 * clean.run();
 * }</pre>
 */
public final class CleanTask implements Task {

    /**
     * 执行清理任务，递归删除当前目录下 {@code build} 和 {@code dist} 目录及所有内容。
     * 目录不存在时自动跳过。
     *
     * @throws IOException 删除目录或文件过程中发生 IO 错误
     */
    @Override
    public void run() throws IOException {
        deleteDir(Path.of("build"));
        deleteDir(Path.of("dist"));
        System.out.println("[clean] done.");
    }

    /**
     * 递归删除指定目录及其所有子文件和子目录。
     * 若目标目录不存在，则直接返回。
     *
     * @param dir 需删除的目录路径
     * @throws IOException 删除目录或文件过程中发生 IO 错误
     */
    private void deleteDir(Path dir) throws IOException {
        if (Files.notExists(dir)) return;
        try (var stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder()) // 先删子文件后删父目录
                    .forEach(p -> {
                        try {
                            if (p.equals(dir)) return;
                            Files.delete(p);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }
}
