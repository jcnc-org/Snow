package org.jcnc.snow.pkg.tasks;

import org.jcnc.snow.pkg.model.Project;
import org.jcnc.snow.pkg.utils.SnowExample;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * GenerateTask 负责项目标准目录结构及示例文件的自动生成，是项目脚手架功能的实现类。
 * <p>
 * 根据 {@link Project} 元数据，自动创建如下内容：
 * <ul>
 *   <li>src/                —— 源码根目录</li>
 *   <li>src/{group package} —— 按 group 创建的包路径（如 com.example → src/com/example/）</li>
 *   <li>test/               —— 测试源码目录</li>
 *   <li>build/              —— 编译输出目录</li>
 *   <li>dist/               —— 打包输出目录</li>
 *   <li>src/{group}/main.snow —— 示例入口模块</li>
 *   <li>src/{group}/OS.snow   —— 系统库模块</li>
 * </ul>
 * 已存在的目录或文件不会覆盖。
 */
public record GenerateTask(Project project) implements Task {

    /**
     * 构造 GenerateTask 实例。
     *
     * @param project 项目信息元数据对象
     */
    public GenerateTask {
    }

    /**
     * 执行脚手架生成流程，创建标准目录和示例文件。
     * <ul>
     *   <li>如相关目录不存在则自动创建</li>
     *   <li>如设置 group，则在 src 下建立包路径</li>
     *   <li>生成 main.snow、OS.snow 示例文件</li>
     *   <li>过程输出进度提示</li>
     * </ul>
     *
     * @throws IOException 创建目录或写文件发生 IO 错误
     */
    @Override
    public void run() throws IOException {
        Path root = Paths.get(".").toAbsolutePath();

        List<Path> dirs = new ArrayList<>(List.of(
                root.resolve("src"),
                root.resolve("test"),
                root.resolve("build"),
                root.resolve("dist")
        ));

        String group = project != null ? project.getGroup() : null;
        Path srcDir = root.resolve("src");
        Path packageDir = srcDir;
        if (group != null && !group.isBlank()) {
            packageDir = srcDir.resolve(group.replace('.', '/'));
            dirs.add(packageDir);
        }

        for (Path dir : dirs) {
            if (Files.notExists(dir)) {
                Files.createDirectories(dir);
                System.out.println("[generate] created directory " + root.relativize(dir));
            }
        }

        Path mainSnow = packageDir.resolve("main.snow");
        if (Files.notExists(mainSnow)) {
            Files.writeString(mainSnow, SnowExample.getMainModule());
            System.out.println("[generate] created " + root.relativize(mainSnow));
        }

        Path osSnow = packageDir.resolve("OS.snow");
        if (Files.notExists(osSnow)) {
            Files.writeString(osSnow, SnowExample.getOsModule());
            System.out.println("[generate] created " + root.relativize(osSnow));
        }

        System.out.println("[generate] project scaffold is ready.");
    }
}
