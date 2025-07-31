package org.jcnc.snow.compiler.ir.builder;

import org.jcnc.snow.compiler.ir.core.IRFunction;
import org.jcnc.snow.compiler.ir.core.IRProgram;
import org.jcnc.snow.compiler.parser.ast.FunctionNode;
import org.jcnc.snow.compiler.parser.ast.ModuleNode;
import org.jcnc.snow.compiler.parser.ast.base.Node;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;

import java.util.ArrayList;
import java.util.List;

/**
 * IRProgramBuilder 负责将 AST 根节点(如模块、函数、顶层语句)转换为可执行的 IRProgram 实例。
 * <p>
 * 主要职责:
 * <ul>
 *   <li>遍历 AST 根节点，根据类型分别处理(模块、函数、顶层语句)。</li>
 *   <li>对模块内的函数添加全限定名，并在函数体前注入全局变量声明。</li>
 *   <li>将单独的顶层语句封装为特殊的 "_start" 函数。</li>
 * </ul>
 */
public final class IRProgramBuilder {

    /**
     * 将解析生成的 AST 根节点列表转换为 IRProgram。
     *
     * @param roots 含 ModuleNode、FunctionNode 或 StatementNode 的顶层 AST 根节点列表
     * @return 包含所有转换后 IRFunction 的 IRProgram 对象
     * @throws IllegalStateException 遇到不支持的顶层节点类型时抛出
     */
    public IRProgram buildProgram(List<Node> roots) {
        IRProgram irProgram = new IRProgram();
        for (Node node : roots) {
            switch (node) {
                case ModuleNode moduleNode -> {
                    // 处理模块节点:遍历其中所有函数，统一用“模块名.函数名”作为全限定名
                    for (FunctionNode f : moduleNode.functions()) {
                        irProgram.add(buildFunctionWithGlobals(moduleNode, f));
                    }
                }
                case FunctionNode functionNode ->
                    // 处理顶层函数节点:直接构建为 IRFunction 并加入
                        irProgram.add(buildFunction(functionNode));
                case StatementNode statementNode ->
                    // 处理脚本式顶层语句:封装成 "_start" 函数后构建并添加
                        irProgram.add(buildFunction(wrapTopLevel(statementNode)));
                default ->
                    // 遇到未知类型节点，抛出异常
                        throw new IllegalStateException("Unsupported top-level node: " + node);
            }
        }
        return irProgram;
    }

    /**
     * 构建带有模块全局声明“注入”的函数，并将函数名加上模块前缀，保证模块内函数名唯一。
     * <p>
     * 如果模块有全局声明，则这些声明会被插入到函数体前部。
     *
     * @param moduleNode   当前模块节点
     * @param functionNode 模块中的函数节点
     * @return 包含全局声明、已加前缀函数名的 IRFunction
     */
    private IRFunction buildFunctionWithGlobals(ModuleNode moduleNode, FunctionNode functionNode) {
        // 拼接模块名和函数名，生成全限定名
        String qualifiedName = moduleNode.name() + "." + functionNode.name();
        // 若无全局声明，仅重命名后直接构建
        if (moduleNode.globals() == null || moduleNode.globals().isEmpty()) {
            return buildFunction(renameFunction(functionNode, qualifiedName));
        }
        // 若有全局声明，插入到函数体最前面
        List<StatementNode> newBody = new ArrayList<>(moduleNode.globals().size() + functionNode.body().size());
        newBody.addAll(moduleNode.globals());
        newBody.addAll(functionNode.body());
        FunctionNode wrapped = new FunctionNode(
                qualifiedName,
                functionNode.parameters(),
                functionNode.returnType(),
                newBody,
                functionNode.context()
        );
        return buildFunction(wrapped);
    }

    /**
     * 生成一个重命名的 FunctionNode(只修改函数名，其他属性保持不变)。
     *
     * @param fn      原始函数节点
     * @param newName 新的函数名(通常为全限定名)
     * @return 重命名后的 FunctionNode
     */
    private FunctionNode renameFunction(FunctionNode fn, String newName) {
        return new FunctionNode(
                newName,
                fn.parameters(),
                fn.returnType(),
                fn.body(),
                fn.context()
        );
    }

    /**
     * 构建 IRFunction。
     *
     * @param functionNode 要转换的函数节点
     * @return 转换结果 IRFunction
     */
    private IRFunction buildFunction(FunctionNode functionNode) {
        return new FunctionBuilder().build(functionNode);
    }

    /**
     * 将顶层语句节点封装成特殊的 "_start" 函数。
     * <p>
     * 这对于脚本式文件支持至关重要(即文件最外层直接写语句)。
     *
     * @param stmt 要封装的顶层语句
     * @return 包装成 FunctionNode 的 "_start" 函数
     */
    private FunctionNode wrapTopLevel(StatementNode stmt) {
        return new FunctionNode(
                "_start",
                List.of(),
                "void",
                List.of(stmt),
                new NodeContext(-1, -1, "")
        );
    }
}
