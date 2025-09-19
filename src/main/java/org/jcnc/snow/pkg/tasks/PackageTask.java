package org.jcnc.snow.pkg.tasks;

import org.jcnc.snow.pkg.model.Project;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * PackageTask 负责将项目的编译输出目录（如 {@code build/classes}）打包为 .ice 文件。
 * <p>
 * 一般用于项目自动化构建流程的打包阶段，仅会打包 {@code build/classes} 目录下所有普通文件，
 * 保持目录结构，不包含其他目录内容。
 * <ul>
 *   <li>输出文件为 {@code dist/artifact-version.ice}</li>
 *   <li>若 dist 目录不存在会自动创建</li>
 *   <li>如 {@code build/classes} 不存在，则不会生成包</li>
 * </ul>
 * 已实现 {@link Task} 接口，可用于 CLI 或自动化流程。
 */
public record PackageTask(Project project) implements Task {

    /**
     * 构造 PackageTask 实例。
     *
     * @param project 目标项目元数据
     */
    public PackageTask {
    }

    /**
     * 执行打包流程，将 {@code build/classes} 目录下所有普通文件压缩为 {@code dist/artifact-version.ice}。
     * <ul>
     *   <li>自动创建 dist 目录（如不存在）</li>
     *   <li>保持 {@code build/classes} 下的相对目录结构</li>
     *   <li>如无 {@code build/classes} 则跳过，不生成包</li>
     * </ul>
     *
     * @throws Exception 打包或文件操作发生异常
     */
    @Override
    public void run() throws Exception {
        String artifact = project.getArtifact();
        String version = project.getVersion();
        String fileName = artifact + "-" + version + ".ice";
        Path distDir = Path.of("dist");
        Files.createDirectories(distDir);
        Path packageFile = distDir.resolve(fileName);

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(packageFile))) {
            Path classesDir = Path.of("build/classes");
            if (Files.exists(classesDir)) {
                try (Stream<Path> stream = Files.walk(classesDir)) {
                    stream.filter(Files::isRegularFile)
                            .forEach(p -> {
                                try {
                                    zos.putNextEntry(new ZipEntry(classesDir.relativize(p).toString()));
                                    Files.copy(p, zos);
                                    zos.closeEntry();
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            });
                }
            }
        }
        System.out.println("[package] created " + packageFile);
    }
}
