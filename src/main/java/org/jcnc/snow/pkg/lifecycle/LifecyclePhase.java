package org.jcnc.snow.pkg.lifecycle;

/**
 * 定义了典型软件包生命周期的各个阶段。
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
