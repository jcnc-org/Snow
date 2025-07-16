package org.jcnc.snow.compiler.semantic.core;

import org.jcnc.snow.compiler.parser.ast.ModuleNode;
import org.jcnc.snow.compiler.parser.ast.base.Node;
import org.jcnc.snow.compiler.semantic.error.SemanticError;
import org.jcnc.snow.compiler.semantic.utils.SemanticAnalysisReporter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * {@code SemanticAnalyzerRunner} 是语义分析阶段的统一入口与调度器。
 * <p>
 * 功能职责: 
 * <ul>
 *   <li>从原始 AST 列表中过滤并收集所有 {@link ModuleNode} 节点，作为模块分析的起点；</li>
 *   <li>调用 {@link SemanticAnalyzer} 对所有模块节点执行完整语义分析流程；</li>
 *   <li>汇总并报告所有 {@link SemanticError}；如有语义错误，自动中止编译流程，防止后续崩溃。</li>
 * </ul>
 * <p>
 * 推荐使用方式: 
 * <pre>
 *     SemanticAnalyzerRunner.runSemanticAnalysis(ast, true);
 * </pre>
 * <p>
 * 该类是实现 SCompiler “所有错误一次性输出，且错误即终止” 语义分析约束的关键。
 */
public class SemanticAnalyzerRunner {

    /**
     * 对输入的语法树执行语义分析并自动报告。
     *
     * @param ast     根节点列表（应包含一个或多个 {@link ModuleNode}）
     * @param verbose 是否启用详细日志（将控制内部 {@link Context#log(String)} 的行为）
     */
    public static void runSemanticAnalysis(List<Node> ast, boolean verbose) {
        // 1. 从 AST 列表中过滤所有模块节点 ModuleNode
        List<ModuleNode> modules = ast.stream()
                .filter(ModuleNode.class::isInstance) // 保留类型为 ModuleNode 的节点
                .map(ModuleNode.class::cast)          // 转换为 ModuleNode
                .collect(Collectors.toList());        // 收集为 List<ModuleNode>

        // 2. 调用语义分析器，对所有模块进行全流程语义分析，返回错误列表
        List<SemanticError> errors = new SemanticAnalyzer(verbose).analyze(modules);

        // 3. 统一报告全部语义错误；如有错误则自动终止编译（System.exit）
        SemanticAnalysisReporter.reportAndExitIfNecessary(errors);
    }
}
