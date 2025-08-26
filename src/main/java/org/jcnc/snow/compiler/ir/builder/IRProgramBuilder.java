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
import java.util.List;

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

    // ===================== 全局常量收集 =====================

    /**
     * 扫描所有模块节点，将其中声明的 const 全局变量（compile-time 常量）
     * 以 "模块名.常量名" 形式注册到全局常量表。
     * <p>
     * 只处理直接字面量（数字、字符串、布尔），暂不支持复杂表达式。
     *
     * @param roots 所有顶层 AST 节点
     */
    private void preloadGlobals(List<Node> roots) {
        for (Node n : roots) {
            if (n instanceof ModuleNode mod) {
                String moduleName = mod.name();
                if (mod.globals() == null) continue;
                for (DeclarationNode decl : mod.globals()) {
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
     * <p>
     * 用于从表达式节点中解析出直接可用于全局常量表注册的 Java 值，
     * 支持以下字面量类型：
     * <ul>
     *   <li>整数与浮点（可带下划线分隔符，支持类型后缀 b/s/l/f/d，自动转换为对应 Java 基本类型）</li>
     *   <li>字符串字面量，直接返回内容</li>
     *   <li>布尔字面量，true 返回 1，false 返回 0（用于数值语境）</li>
     * </ul>
     * 不支持的表达式或解析失败将返回 null。
     *
     * @param expr 字面量表达式节点，通常为 NumberLiteralNode/StringLiteralNode/BoolLiteralNode
     * @return 提取出的 Java 值，可为 Integer/Byte/Short/Long/Double/String/Integer(布尔) 或 null
     */
    private Object evalLiteral(ExpressionNode expr) {
        return switch (expr) {
            case NumberLiteralNode num -> {
                String raw = num.value();
                // 1. 去掉下划线分隔符，便于解析（如 1_000_000）
                String s = raw.replace("_", "");
                // 2. 提取类型后缀（b/s/l/f/d），若有则去除
                char last = Character.toLowerCase(s.charAt(s.length() - 1));
                String core = switch (last) {
                    case 'b', 's', 'l', 'f', 'd' -> s.substring(0, s.length() - 1);
                    default -> s;
                };
                try {
                    // 3. 若为浮点字面量或科学计数法，直接转 double
                    if (core.contains(".") || core.contains("e") || core.contains("E")) {
                        double dv = Double.parseDouble(core);
                        yield dv;
                    }
                    // 4. 整数字面量，先转 long，再根据后缀决定精度
                    long lv = Long.parseLong(core);
                    yield switch (last) {
                        case 'b' -> (byte) lv;   // 末尾 b，转 byte
                        case 's' -> (short) lv;  // 末尾 s，转 short
                        case 'l' -> lv;          // 末尾 l，转 long
                        default -> (int) lv;    // 无后缀，转 int
                    };
                } catch (NumberFormatException ignore) {
                    // 解析失败，返回 null
                    yield null;
                }
            }
            case StringLiteralNode str -> str.value();        // 字符串字面量，返回内容
            case BoolLiteralNode b -> b.getValue() ? 1 : 0;   // 布尔字面量，true=1，false=0
            default -> null;                                  // 其它类型不支持
        };
    }

    // ===================== IRFunction 构建辅助 =====================

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
