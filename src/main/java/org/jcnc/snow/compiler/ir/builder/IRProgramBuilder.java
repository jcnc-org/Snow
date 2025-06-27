package org.jcnc.snow.compiler.ir.builder;

import org.jcnc.snow.compiler.ir.core.IRFunction;
import org.jcnc.snow.compiler.ir.core.IRProgram;
import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.parser.ast.ModuleNode;
import org.jcnc.snow.compiler.parser.ast.base.Node;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;

import java.util.List;

/**
 * 本类负责将解析生成的 AST 根节点列表转换为可执行的 IRProgram。
 *
 * <p>主要职责：
 * <ul>
 *   <li>遍历输入的顶层节点，识别 ModuleNode、FunctionNode 及脚本式顶层 StatementNode；</li>
 *   <li>对 ModuleNode 中的所有函数节点调用 FunctionBuilder 构建 IRFunction 并添加至 IRProgram；</li>
 *   <li>对单独的 FunctionNode 节点直接构建并纳入 IRProgram；</li>
 *   <li>对顶层脚本式 StatementNode 自动封装为名称固定的“_start”函数，再行构建并纳入 IRProgram；</li>
 *   <li>对不支持的节点类型抛出 IllegalStateException，以确保编译流程严谨。</li>
 * </ul>
 */
public final class IRProgramBuilder {

    /**
     * 构建完整的 IRProgram 实例。
     *
     * @param roots 含 ModuleNode、FunctionNode 或 StatementNode 的顶层 AST 根节点列表
     * @return 包含所有转换后 IRFunction 的 IRProgram 对象
     * @throws IllegalStateException 遇到不支持的顶层节点类型时抛出
     */
    public IRProgram buildProgram(List<Node> roots) {
        IRProgram irProgram = new IRProgram();
        for (Node node : roots) {
            switch (node) {
                case ModuleNode moduleNode ->
                    // 模块节点：批量构建并添加模块内所有函数
                        moduleNode.functions().forEach(f -> irProgram.add(buildFunction(f)));
                case FunctionNode functionNode ->
                    // 顶层函数节点：直接构建并添加
                        irProgram.add(buildFunction(functionNode));
                case StatementNode statementNode ->
                    // 脚本式顶层语句：封装为“_start”函数后构建并添加
                        irProgram.add(buildFunction(wrapTopLevel(statementNode)));
                default ->
                    // 严格校验节点类型，遇不支持者立即失败
                        throw new IllegalStateException("Unsupported top-level node: " + node);
            }
        }
        return irProgram;
    }

    /**
     * 利用 FunctionBuilder 将 FunctionNode 转换为 IRFunction。
     *
     * @param functionNode 待构建的 AST FunctionNode
     * @return 构建完成的 IRFunction 实例
     */
    private IRFunction buildFunction(FunctionNode functionNode) {
        return new FunctionBuilder().build(functionNode);
    }

    /**
     * 将单个脚本式顶层 StatementNode 封装为名称固定的“_start”函数节点。
     *
     * <p>封装规则：
     * <ul>
     *   <li>函数名固定为“_start”；</li>
     *   <li>返回类型设为 null，由后续流程处理；</li>
     *   <li>参数列表为空；</li>
     *   <li>函数主体仅包含传入的单条语句。</li>
     * </ul>
     *
     * @param stmt 待封装的顶层脚本语句节点
     * @return 生成的 FunctionNode，用于后续 IRFunction 构建
     */
    private FunctionNode wrapTopLevel(StatementNode stmt) {
        return new FunctionNode(
                "_start",
                null,
                String.valueOf(List.of()),
                List.of(stmt),
                -1,
                -1,
                ""
        );
    }
}
