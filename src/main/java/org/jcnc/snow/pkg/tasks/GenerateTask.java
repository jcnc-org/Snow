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
 * 项目脚手架生成任务。<br>
 * 根据 {@link Project} 元数据自动创建标准项目目录结构，并生成示例入口文件
 * <code>main.snow</code>。
 *
 * <p>
 * 生成内容包括:
 * <ul>
 *   <li><code>src/</code>                       —— 源码根目录</li>
 *   <li><code>src/{group package}/</code> —— 按 <code>project.group</code> 创建的包路径
 *       （如 <code>com.example</code> → <code>src/com/example/</code>）</li>
 *   <li><code>test/</code>                     —— 测试源码目录</li>
 *   <li><code>build/</code>                     —— 编译输出目录</li>
 *   <li><code>dist/</code>                      —— 打包输出目录</li>
 *   <li><code>src/{group package}/main.snow</code> —— 示例入口文件</li>
 * </ul>
 * 如目录或入口文件已存在，则自动跳过，不会覆盖。
 * </p>
 */
public final class GenerateTask implements Task {

    /**
     * 项目信息元数据
     */
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
     *   <li>若设置了 <code>project.group</code>，则在 <code>src/</code> 下新建对应包路径</li>
     *   <li>示例入口文件 <code>main.snow</code> 写入包路径目录</li>
     *   <li>生成过程输出进度信息</li>
     * </ul>
     *
     * @throws IOException 创建目录或写入文件时发生 IO 错误时抛出
     */
    @Override
    public void run() throws IOException {
        Path root = Paths.get(".").toAbsolutePath();

        /* ---------- 1. 构造待创建目录列表 ---------- */
        List<Path> dirs = new ArrayList<>(List.of(
                root.resolve("src"),
                root.resolve("test"),
                root.resolve("build"),
                root.resolve("dist")
        ));

        /* ---------- 2. 处理 group: 追加包目录 ---------- */
        String group = project != null ? project.getGroup() : null;
        Path srcDir = root.resolve("src");
        Path packageDir = srcDir;  // 默认直接在 src 下
        if (group != null && !group.isBlank()) {
            packageDir = srcDir.resolve(group.replace('.', '/'));
            dirs.add(packageDir);
        }

        /* ---------- 3. 创建目录 ---------- */
        for (Path dir : dirs) {
            if (Files.notExists(dir)) {
                Files.createDirectories(dir);
                System.out.println("[generate] created directory " + root.relativize(dir));
            }
        }

        /* ---------- 4. 写入示例入口文件 main.snow ---------- */
        Path mainSnow = packageDir.resolve("main.snow");
        if (Files.notExists(mainSnow)) {
            Files.writeString(mainSnow, SnowExample.getMainModule());
            System.out.println("[generate] created " + root.relativize(mainSnow));
        }

        /* ---------- 5. 写入系统库文件 os.snow ---------- */
        Path osSnow = packageDir.resolve("OS.snow");
        if (Files.notExists(osSnow)) {
            Files.writeString(osSnow, SnowExample.getOsModule());
            System.out.println("[generate] created " + root.relativize(osSnow));
        }

        System.out.println("[generate] project scaffold is ready.");
    }
}
