package org.jcnc.snow.compiler.ir.builder;

import org.jcnc.snow.compiler.ir.common.GlobalConstTable;
import org.jcnc.snow.compiler.ir.core.IRFunction;
import org.jcnc.snow.compiler.ir.core.IRProgram;
import org.jcnc.snow.compiler.parser.ast.*;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.Node;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * IRProgramBuilder 负责将 AST 顶层节点转换为可执行的 {@link IRProgram}。
 *
 * <ul>
 *   <li>预扫描所有模块，将 <code>declare const</code> 常量登记到全局常量表，支持跨模块常量折叠。</li>
 *   <li>对模块内的函数加上模块前缀，保证命名唯一，并将本模块全局声明注入到函数体前部。</li>
 *   <li>将独立顶层语句自动包装为特殊的 "_start" 函数（脚本模式支持）。</li>
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
        // 预先收集并登记全部模块常量到全局常量表
        preloadGlobals(roots);

        IRProgram irProgram = new IRProgram();
        for (Node node : roots) {
            switch (node) {
                case ModuleNode moduleNode -> {
                    // 处理模块节点：遍历其中所有函数，统一用“模块名.函数名”作为全限定名，避免命名冲突
                    for (FunctionNode f : moduleNode.functions()) {
                        irProgram.add(buildFunctionWithGlobals(moduleNode, f));
                    }
                }
                case FunctionNode functionNode ->
                    // 处理顶层函数节点：直接构建为 IRFunction 并加入
                        irProgram.add(buildFunction(functionNode));
                case StatementNode statementNode ->
                    // 处理脚本式顶层语句：封装成 "_start" 函数后构建并添加
                        irProgram.add(buildFunction(wrapTopLevel(statementNode)));
                default ->
                    // 遇到未知类型节点，抛出异常
                        throw new IllegalStateException("Unsupported top-level node: " + node);
            }
        }
        return irProgram;
    }

    // ===================== 全局常量收集 =====================

    /**
     * 扫描所有模块节点，将其中声明的 const 全局变量（compile-time 常量）
     * 以 "模块名.常量名" 形式注册到全局常量表。
     * 支持跨模块常量折叠，用于后续 IR 生成过程中的常量折叠优化。
     *
     * @param roots AST 顶层节点列表
     */
    private void preloadGlobals(List<Node> roots) {
        for (Node n : roots) {
            if (n instanceof ModuleNode mod) {
                String moduleName = mod.name();
                if (mod.globals() == null) continue;
                for (DeclarationNode decl : mod.globals()) {
                    // 只处理 compile-time 的 const 常量，并要求有初始值
                    if (!decl.isConst() || decl.getInitializer().isEmpty()) continue;
                    ExpressionNode init = decl.getInitializer().get();
                    Object value = evalLiteral(init);
                    if (value != null) {
                        GlobalConstTable.register(moduleName + "." + decl.getName(), value);
                    }
                }
            }
        }
    }

    /**
     * 字面量提取与类型折叠工具。
     * 用于将表达式节点还原为 Java 原生类型（int、long、double、String等），仅支持直接字面量。
     *
     * @param expr 要计算的表达式节点
     * @return 提取到的原生常量值，不支持的情况返回 null
     */
    private Object evalLiteral(ExpressionNode expr) {
        return switch (expr) {
            case NumberLiteralNode num -> {
                String raw = num.value();
                String s = raw.replace("_", "");
                char last = Character.toLowerCase(s.charAt(s.length() - 1));
                String core = switch (last) {
                    case 'b', 's', 'l', 'f', 'd' -> s.substring(0, s.length() - 1);
                    default -> s;
                };
                try {
                    if (core.contains(".") || core.contains("e") || core.contains("E")) {
                        // 浮点数处理
                        yield Double.parseDouble(core);
                    }
                    long lv = Long.parseLong(core);
                    yield switch (last) {
                        case 'b' -> (byte) lv;
                        case 's' -> (short) lv;
                        case 'l' -> lv;
                        default -> (int) lv;
                    };
                } catch (NumberFormatException ignore) {
                    yield null;
                }
            }
            case StringLiteralNode str -> str.value();
            case BoolLiteralNode b -> b.getValue() ? 1 : 0;
            default -> null;
        };
    }

    // ===================== IRFunction 构建辅助 =====================

    /**
     * 构建带有模块全局声明“注入”的函数，并将函数名加上模块前缀，保证模块内函数名唯一。
     * 如果模块有全局声明，则这些声明会被插入到函数体前部（**会过滤掉与参数同名的全局声明**）。
     *
     * @param moduleNode   所属模块节点
     * @param functionNode 待构建的函数节点
     * @return 包含全局声明的 IRFunction
     */
    private IRFunction buildFunctionWithGlobals(ModuleNode moduleNode, FunctionNode functionNode) {
        // 拼接模块名和函数名，生成全限定名
        String qualifiedName = moduleNode.name() + "." + functionNode.name();
        if (moduleNode.globals() == null || moduleNode.globals().isEmpty()) {
            // 无全局声明，直接重命名构建
            return buildFunction(renameFunction(functionNode, qualifiedName));
        }

        // ------- 过滤与参数重名的全局声明 -------
        Set<String> paramNames = new HashSet<>();
        for (ParameterNode p : functionNode.parameters()) {
            paramNames.add(p.name());
        }
        List<StatementNode> filteredGlobals = new ArrayList<>();
        for (DeclarationNode g : moduleNode.globals()) {
            // 避免全局声明和参数重名，优先参数
            if (!paramNames.contains(g.getName())) {
                filteredGlobals.add(g);
            }
        }

        if (filteredGlobals.isEmpty()) {
            // 过滤后已无可插入的全局声明
            return buildFunction(renameFunction(functionNode, qualifiedName));
        }

        // 合并全局声明与函数体，前插全局声明
        List<StatementNode> newBody = new ArrayList<>(filteredGlobals.size() + functionNode.body().size());
        newBody.addAll(filteredGlobals);
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
     * 生成一个重命名的 FunctionNode（只修改函数名，其他属性保持不变）。
     *
     * @param fn      原始函数节点
     * @param newName 新的函数名（全限定名）
     * @return 重命名后的函数节点
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
     * @param functionNode 待构建的 FunctionNode
     * @return 构建后的 IRFunction
     */
    private IRFunction buildFunction(FunctionNode functionNode) {
        return new FunctionBuilder().build(functionNode);
    }

    /**
     * 将顶层语句节点封装成特殊的 "_start" 函数。
     * 主要用于脚本模式支持，使得顶层语句也可以被 IR 执行引擎统一处理。
     *
     * @param stmt 顶层语句节点
     * @return 封装后的 FunctionNode
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
