package org.jcnc.snow.pkg.tasks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * 清理构建输出目录（如 build 和 dist）的任务实现。
 * <p>
 * 实现 {@link Task} 接口，通常用于构建流程中的清理阶段。
 * </p>
 */
public final class CleanTask implements Task {

    /**
     * 执行清理任务，删除 "build" 和 "dist" 目录及其所有内容。
     *
     * @throws IOException 删除目录过程中出现 IO 错误时抛出
     */
    @Override
    public void run() throws IOException {
        deleteDir(Path.of("build"));
        deleteDir(Path.of("dist"));
        System.out.println("[clean] done.");
    }

    /**
     * 递归删除指定目录及其所有子文件和子目录。
     * 使用 try-with-resources 自动关闭文件流，避免资源泄漏。
     *
     * @param dir 需要删除的目录路径
     * @throws IOException 删除过程中出现 IO 错误时抛出
     */
    private void deleteDir(Path dir) throws IOException {
        if (Files.notExists(dir)) return;
        try (var stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder()) // 先删子文件，后删父目录
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }
}
