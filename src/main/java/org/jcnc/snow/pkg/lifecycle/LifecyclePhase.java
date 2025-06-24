package org.jcnc.snow.pkg.lifecycle;

/**
 * 定义典型软件包生命周期的各个阶段枚举。
 * <p>
 * 用于区分构建、依赖、发布等不同阶段的任务调度与管理。
 * </p>
 */
public enum LifecyclePhase {
    /** 初始化阶段 */
    INIT,
    /** 解析依赖阶段 */
    RESOLVE_DEPENDENCIES,
    /** 编译阶段 */
    COMPILE,
    /** 打包阶段 */
    PACKAGE,
    /** 发布阶段 */
    PUBLISH,
    /** 清理阶段 */
    CLEAN
}
