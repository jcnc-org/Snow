package org.jcnc.snow.pkg.tasks;

import org.jcnc.snow.pkg.model.Project;

/**
 * 发布项目构件到远程仓库的任务实现。
 * <p>
 * 实现 {@link Task} 接口，通常用于构建流程中的发布阶段。
 * 当前仅输出发布提示，尚未实现实际上传功能。
 * </p>
 */
public final class PublishTask implements Task {

    /**
     * 目标项目元数据
     */
    private final Project project;

    /**
     * 创建发布任务。
     *
     * @param project 目标项目元数据
     */
    public PublishTask(Project project) {
        this.project = project;
    }

    /**
     * 执行发布任务。目前仅打印发布提示信息，未实现实际上传逻辑。
     *
     * @throws Exception 预留，未来实现上传逻辑时可能抛出异常
     */
    @Override
    public void run() throws Exception {
        // TODO: 实现上传到仓库（如 HTTP PUT/POST）
        System.out.println("[publish] uploading artifact " + project.getArtifact() + "-" + project.getVersion());
    }
}
