package org.jcnc.snow.pkg.tasks;

import org.jcnc.snow.pkg.model.Project;
import org.jcnc.snow.pkg.utils.SnowExample;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 项目脚手架生成任务。<br>
 * 根据 {@link Project} 元数据自动创建标准项目目录结构，并生成示例入口文件 <code>src/main.snow</code>。
 * <p>
 * 生成内容包括：
 * <ul>
 *   <li>src/          —— 源码目录</li>
 *   <li>test/         —— 测试源码目录</li>
 *   <li>build/        —— 编译输出目录</li>
 *   <li>dist/         —— 打包输出目录</li>
 *   <li>src/main.snow —— “Hello, Snow!” 示例入口</li>
 * </ul>
 * 如目录或入口文件已存在，则自动跳过，不会覆盖。
 * </p>
 */
public final class GenerateTask implements Task {

    /** 项目信息元数据 */
    private final Project project;

    /**
     * 创建项目生成任务。
     *
     * @param project 项目信息元数据对象
     */
    public GenerateTask(Project project) {
        this.project = project;
    }

    /**
     * 执行脚手架生成流程，创建标准目录和入口示例文件。
     * <ul>
     *   <li>若相关目录不存在则创建</li>
     *   <li>若 <code>src/main.snow</code> 不存在则写入模板</li>
     *   <li>生成过程输出进度信息</li>
     * </ul>
     *
     * @throws IOException 创建目录或写入文件时发生 IO 错误时抛出
     */
    @Override
    public void run() throws IOException {
        Path root = Paths.get(".").toAbsolutePath();

        // 创建标准目录
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

        // 创建 src/main.snow 示例入口文件
        Path mainSnow = root.resolve("src").resolve("main.snow");
        if (Files.notExists(mainSnow)) {
            Files.writeString(mainSnow, SnowExample.getMainModule());
            System.out.println("[generate] created src/main.snow");
        }

        System.out.println("[generate] project scaffold is ready.");
    }
}
