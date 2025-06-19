package org.jcnc.snow.pkg.tasks;

import org.jcnc.snow.pkg.model.Project;

import java.nio.file.Path;

/**
 * 编译项目源代码的任务实现。
 * <p>
 * 实现 {@link Task} 接口，用于构建流程中的编译阶段。当前仅为示例，未集成实际编译器。
 * </p>
 */
public final class CompileTask implements Task {

    /** 待编译的项目 */
    private final Project project;

    /**
     * 创建 CompileTask 实例。
     *
     * @param project 目标项目
     */
    public CompileTask(Project project) {
        this.project = project;
    }

    /**
     * 执行编译任务，打印源代码目录和输出目录。
     * <p>
     * 实际编译尚未实现（TODO）。
     * </p>
     *
     * @throws Exception 预留，未来集成编译器可能抛出异常
     */
    @Override
    public void run() throws Exception {
        // 获取源码目录和输出目录，默认分别为 "src" 和 "build/classes"
        Path srcDir = Path.of(project.getProperties().getOrDefault("src_dir", "src"));
        Path outDir = Path.of(project.getProperties().getOrDefault("output_dir", "build/classes"));
        System.out.println("[compile] sources=" + srcDir + " output=" + outDir);
        // TODO: 集成实际的编译器
    }
}
