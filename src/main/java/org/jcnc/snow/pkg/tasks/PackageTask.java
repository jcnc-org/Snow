package org.jcnc.snow.pkg.tasks;

import org.jcnc.snow.pkg.model.Project;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 项目打包任务，将编译输出目录（如 build/classes）打包为 .ice 文件。
 * <p>
 * 实现 {@link Task} 接口，通常用于构建流程中的打包阶段。
 * </p>
 */
public final class PackageTask implements Task {

    /** 目标项目 */
    private final Project project;

    /**
     * 创建 PackageTask 实例。
     *
     * @param project 目标项目
     */
    public PackageTask(Project project) {
        this.project = project;
    }

    /**
     * 执行打包任务，将编译输出目录压缩为 artifact-version.ice 文件。
     *
     * @throws Exception 打包过程中出现 IO 或其他异常时抛出
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
            // 仅将编译输出目录打包
            Path classesDir = Path.of("build/classes");
            if (Files.exists(classesDir)) {
                // 使用 try-with-resources 正确关闭 Stream<Path>
                try (Stream<Path> stream = Files.walk(classesDir)) {
                    stream.filter(Files::isRegularFile)
                            .forEach(p -> {
                                try {
                                    // 将文件以相对路径加入压缩包
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
