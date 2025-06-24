package org.jcnc.snow.pkg.tasks;

import org.jcnc.snow.pkg.model.Project;
import org.jcnc.snow.pkg.utils.SnowExampleTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 任务：依据 {@link Project} 元数据创建标准项目目录。
 * <p>
 * 生成内容：
 * <ul>
 *   <li>src/          —— 源码目录</li>
 *   <li>test/         —— 测试源码目录</li>
 *   <li>build/        —— 编译输出目录</li>
 *   <li>dist/         —— 打包输出目录</li>
 *   <li>src/main.snow —— “Hello, Snow!” 示例入口</li>
 * </ul>
 */
public final class GenerateTask implements Task {

    private final Project project;

    public GenerateTask(Project project) {
        this.project = project;
    }

    @Override
    public void run() throws IOException {
        Path root = Paths.get(".").toAbsolutePath();

        /* -------- 创建目录 -------- */
        List<Path> dirs = List.of(
                root.resolve("src"),
                root.resolve("test"),
                root.resolve("build"),
                root.resolve("dist")
        );

        for (Path dir : dirs) {
            if (Files.notExists(dir)) {
                Files.createDirectories(dir);
                System.out.println("[generate] created directory " + root.relativize(dir));
            }
        }

        /* -------- 创建示例入口文件 -------- */
        Path mainSnow = root.resolve("src").resolve("main.snow");
        if (Files.notExists(mainSnow)) {
            Files.writeString(mainSnow, SnowExampleTemplate.getMainModule());
            System.out.println("[generate] created src/main.snow");
        }

        System.out.println("[generate] project scaffold is ready.");
    }
}
