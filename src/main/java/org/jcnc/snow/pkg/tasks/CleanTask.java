package org.jcnc.snow.pkg.tasks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * 用于清理构建输出目录（如 {@code build} 和 {@code dist}）的任务实现类。
 * <p>
 * 实现 {@link Task} 接口，常用于自动化构建流程的清理阶段，负责递归删除指定的构建产物目录。
 * </p>
 * <p>
 * 本类为无状态实现，线程安全。
 * </p>
 *
 * <p><b>示例用法：</b></p>
 * <pre>{@code
 * Task clean = new CleanTask();
 * clean.run();
 * }</pre>
 */
public final class CleanTask implements Task {

    /**
     * 执行清理任务，递归删除当前目录下的 {@code build} 和 {@code dist} 目录及其所有内容。
     * 如果目标目录不存在，则跳过不处理。
     *
     * @throws IOException 删除目录或文件过程中发生 IO 错误时抛出
     */
    @Override
    public void run() throws IOException {
        deleteDir(Path.of("build"), false);
        deleteDir(Path.of("dist"), false);

        System.out.println("[clean] done.");
    }

    /**
     * 递归删除指定目录下的所有子文件和子目录。
     * 如需删除指定目录本身可将第二个参数 <span>containSelf</span> 设置为 true
     * <p>
     * 若目录不存在，则直接返回。
     * </p>
     * <p>
     * 内部使用 try-with-resources 保证文件流自动关闭，避免资源泄漏。
     * </p>
     *
     * @param dir 需要删除的目录路径
     * @param containSelf 是否删除指定目录本身
     * @throws IOException 删除目录或文件过程中发生 IO 错误时抛出
     */
    private void deleteDir(Path dir, boolean containSelf) throws IOException {
        if (Files.notExists(dir)) return;
        try (var stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder()) // 先删子文件，后删父目录
                    .forEach(p -> {
                        try {
                            if (!containSelf && p == dir) {
                                return;
                            }
                            Files.delete(p);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }
}
