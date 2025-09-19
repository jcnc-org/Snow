package org.jcnc.snow.pkg.tasks;

import org.jcnc.snow.pkg.model.Project;

/**
 * PublishTask 负责发布项目构件到远程仓库，通常用于构建流程中的发布阶段。
 * <p>
 * 已实现 {@link Task} 接口。目前仅输出发布提示，未实现实际上传功能，预留后续扩展（如 HTTP PUT/POST）。
 * </p>
 * <ul>
 *   <li>project: 目标项目元数据信息</li>
 *   <li>支持 CLI 或自动化调用</li>
 * </ul>
 */
public record PublishTask(Project project) implements Task {

    /**
     * 构造 PublishTask 实例。
     *
     * @param project 目标项目元数据
     */
    public PublishTask {
    }

    /**
     * 执行发布任务。当前仅打印发布提示信息，未实现实际上传逻辑。
     *
     * @throws Exception 预留，未来实现上传逻辑时可能抛出异常
     */
    @Override
    public void run() throws Exception {
        // TODO: 实现上传到远程仓库
        System.out.println("[publish] uploading artifact " + project.getArtifact() + "-" + project.getVersion());
    }
}
