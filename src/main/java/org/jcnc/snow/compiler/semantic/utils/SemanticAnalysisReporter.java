package org.jcnc.snow.compiler.semantic.utils;

import org.jcnc.snow.compiler.semantic.error.SemanticError;

import java.util.List;

/**
 * {@code SemanticAnalysisReporter} 用于在语义分析结束后汇总并打印所有收集到的
 * {@link SemanticError}。为了同时满足“完整错误收集”与“按需快速失败”两种使用场景，
 * 现在提供两个公共 API：
 * <ul>
 *   <li>{@link #report(List)} ‑ 仅打印，不终止；</li>
 *   <li>{@link #reportAndExitIfNecessary(List)} ‑ 若存在错误则 <b>打印并退出</b>。</li>
 * </ul>
 * 调用方可根据需求选择合适方法。
 */
public final class SemanticAnalysisReporter {

    private SemanticAnalysisReporter() { }

    /**
     * 打印语义分析结果；<b>不会</b>退出进程。
     *
     * @param errors 语义分析阶段收集到的错误列表（允许为 {@code null}）
     */
    public static void report(List<SemanticError> errors) {
        if (hasErrors(errors)) {
            System.err.println("语义分析发现 " + errors.size() + " 个错误：");
            errors.forEach(err -> System.err.println("  " + err));
        } else {
            System.out.println("\n## 语义分析通过，没有发现错误\n");
        }
    }

    /**
     * 打印语义分析结果；如有错误立即以状态码 <code>1</code> 结束进程。
     * 适用于 CLI 工具需要立即中止后续编译阶段的场景。
     *
     * @param errors 语义分析阶段收集到的错误列表（允许为 {@code null}）
     */
    public static void reportAndExitIfNecessary(List<SemanticError> errors) {
        report(errors);
        if (hasErrors(errors)) {
            System.exit(1);
        }
    }

    private static boolean hasErrors(List<SemanticError> errors) {
        return errors != null && !errors.isEmpty();
    }
}