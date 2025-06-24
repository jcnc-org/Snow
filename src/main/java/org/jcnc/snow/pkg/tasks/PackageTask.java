package org.jcnc.snow.pkg.tasks;

import org.jcnc.snow.pkg.model.Project;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 项目打包任务，将编译输出目录（如 <code>build/classes</code>）打包为 .ice 文件。
 * <p>
 * 实现 {@link Task} 接口，通常用于构建流程的打包阶段。<br>
 * 只会打包 build/classes 目录下所有文件，不含其他目录。
 * </p>
 * <p>
 * 输出文件位于 dist 目录，命名为 artifact-version.ice。
 * </p>
 */
public final class PackageTask implements Task {

    /** 目标项目元数据 */
    private final Project project;

    /**
     * 创建打包任务。
     *
     * @param project 目标项目元数据
     */
    public PackageTask(Project project) {
        this.project = project;
    }

    /**
     * 执行打包任务，将 build/classes 目录下所有文件压缩为 dist/artifact-version.ice。
     * <ul>
     *   <li>若输出目录 dist 不存在会自动创建</li>
     *   <li>只打包 build/classes 下所有普通文件（保持相对目录结构）</li>
     *   <li>如无 build/classes 则不会生成包</li>
     * </ul>
     *
     * @throws Exception 打包过程中发生 IO 或其他异常时抛出
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
            // 仅将 build/classes 目录打包
            Path classesDir = Path.of("build/classes");
            if (Files.exists(classesDir)) {
                // 遍历所有文件并写入 zip
                try (Stream<Path> stream = Files.walk(classesDir)) {
                    stream.filter(Files::isRegularFile)
                            .forEach(p -> {
                                try {
                                    // 以 classesDir 为根的相对路径存入 zip
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
