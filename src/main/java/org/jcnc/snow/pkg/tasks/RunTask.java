package org.jcnc.snow.pkg.tasks;

import org.jcnc.snow.vm.VMLauncher;

/**
 * 任务: 执行已编译的 VM 字节码文件（.water）。
 * <p>
 * 作为 CLI、IDE 插件或其他宿主环境启动虚拟机的统一入口，<br>
 * 通过调用 {@link VMLauncher#main(String[])} 启动 VM 并执行指定程序。
 * </p>
 */
public final class RunTask implements Task {

    /**
     * 传递给虚拟机的完整参数列表（第一个应为 .water 文件路径）
     */
    private final String[] args;

    /**
     * 创建运行任务。
     *
     * @param args VM 参数数组（第一个为 .water 程序路径，其后为可选参数）
     */
    public RunTask(String... args) {
        this.args = args;
    }

    /**
     * 执行运行任务。内部委托 {@link VMLauncher#main(String[])} 启动 VM。
     * <ul>
     *   <li>如果参数为空则抛出 {@link IllegalArgumentException}</li>
     *   <li>异常由虚拟机本身抛出，直接透出</li>
     * </ul>
     *
     * @throws Exception 虚拟机启动或运行期间抛出的异常
     */
    @Override
    public void run() throws Exception {
        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("VM run requires at least the program file path.");
        }
        VMLauncher.main(args);
    }
}
