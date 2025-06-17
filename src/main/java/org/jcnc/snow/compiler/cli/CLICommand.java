package org.jcnc.snow.compiler.cli;

/**
 * <p>
 * 所有子命令（如 compile、run 等）都必须实现的命令接口。
 * </p>
 * <ul>
 *   <li>实现类应当是无状态（stateless）、线程安全（thread-safe）的。</li>
 *   <li>可通过 {@link java.util.ServiceLoader ServiceLoader} 自动发现，或直接在 {@link SnowCLI} 注册。</li>
 * </ul>
 * <p>
 */
public interface CLICommand {

    /**
     * 获取命令的名称（如 "compile"、"run"）。
     *
     * @return 命令名字符串
     */
    String name();

    /**
     * 获取命令的一行简介（用于 help 列表）。
     *
     * @return 命令描述字符串
     */
    String description();

    /**
     * 打印命令的专用 usage 信息（可选实现）。
     * 可覆盖此方法自定义帮助信息，默认无操作。
     */
    default void printUsage() {
    }

    /**
     * 执行命令逻辑。
     *
     * @param args 传递给子命令的参数（不含命令名本身）
     * @return 进程退出码（0 为成功，非0为错误）
     * @throws Exception 可抛出任意异常，框架会统一捕获和输出
     */
    int execute(String[] args) throws Exception;
}
