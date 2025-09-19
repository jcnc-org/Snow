package org.jcnc.snow.pkg.tasks;

import org.jcnc.snow.vm.VMLauncher;

/**
 * RunTask 负责执行已编译的 VM 字节码程序（.water 文件），
 * 通常用于 CLI、IDE 插件或自动化流程中统一启动虚拟机。
 * <p>
 * 通过调用 {@link VMLauncher#main(String[])} 启动虚拟机并运行指定程序。
 * </p>
 * <ul>
 *   <li>args：传递给 VM 的参数数组，第一个应为 .water 程序路径</li>
 * </ul>
 */
public record RunTask(String... args) implements Task {

    /**
     * 构造 RunTask 实例。
     *
     * @param args VM 启动参数数组（第一个为 .water 程序路径，其后为可选参数）
     */
    public RunTask {
    }

    /**
     * 执行 VM 启动任务，委托 {@link VMLauncher#main(String[])} 启动虚拟机。
     * <ul>
     *   <li>如未传递参数，将抛出 {@link IllegalArgumentException}</li>
     *   <li>虚拟机运行期间的异常直接透出</li>
     * </ul>
     *
     * @throws Exception 虚拟机启动或运行期间的异常
     */
    @Override
    public void run() throws Exception {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("VM run requires at least the program file path.");
        }
        VMLauncher.main(args);
    }
}
