package org.jcnc.snow.compiler.parser.function;

import org.jcnc.snow.compiler.parser.ast.*;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.Node;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;
import org.jcnc.snow.compiler.parser.utils.ASTJsonSerializer;
import org.jcnc.snow.compiler.parser.utils.JsonFormatter;

import java.util.List;

/**
 * {@code ASTPrinter} 是一个抽象语法树（AST）打印工具类，
 * 提供用于调试和可视化的格式化文本输出与 JSON 序列化能力。
 *
 * <p>
 * 该类支持以缩进方式层级打印任意 AST 节点，包括模块、函数、控制结构等，
 * 适用于开发阶段调试语法树结构或输出结构化语法分析结果。
 * </p>
 *
 * <p>
 * 同时支持将 AST 序列化为 JSON 字符串，并使用 {@link JsonFormatter} 美化输出，
 * 便于与外部工具对接或做进一步分析。
 * </p>
 */
public class ASTPrinter {

    /**
     * 打印整个抽象语法树的节点集合。
     *
     * <p>
     * 每个节点及其子节点将以多行文本形式打印，展示语法树的层级结构。
     * 该方法通常用于打印完整模块、函数集等顶层结构。
     * </p>
     *
     * @param nodes 要打印的 AST 节点列表，通常为模块或顶层语句列表。
     */
    public static void print(List<Node> nodes) {
        for (Node n : nodes) {
            print(n, 0);
        }
    }

    /**
     * 递归打印单个 AST 节点及其子节点，带有格式化缩进。
     *
     * <p>
     * 支持的节点类型包括模块、函数、声明、赋值、条件语句、循环、返回语句、表达式语句等。
     * 若节点类型未显式支持，则退回使用 {@code toString()} 方法进行输出。
     * </p>
     *
     * @param n      要打印的 AST 节点。
     * @param indent 当前缩进层级，每级对应两个空格。
     */
    private static void print(Node n, int indent) {
        String pad = "  ".repeat(indent);

        switch (n) {
            case ModuleNode m -> {
                System.out.println(pad + "module " + m.name());
                for (ImportNode imp : m.imports()) {
                    System.out.println(pad + "  import " + imp.moduleName());
                }
                for (FunctionNode fn : m.functions()) {
                    print(fn, indent + 1);
                }
            }
            case FunctionNode(
                    String name, List<ParameterNode> parameters, String returnType, List<StatementNode> body,
                    NodeContext _
            ) -> {
                System.out.println(pad + "function " + name
                        + "(params=" + parameters + ", return=" + returnType + ")");
                for (StatementNode stmt : body) {
                    print(stmt, indent + 1);
                }
            }
            case DeclarationNode d -> {
                String init = d.getInitializer()
                        .map(Object::toString)
                        .map(s -> " = " + s)
                        .orElse("");
                System.out.println(pad + "declare " + d.getName() + ":" + d.getType() + init);
            }
            case AssignmentNode(String variable, ExpressionNode value, NodeContext _) ->
                    System.out.println(pad + variable + " = " + value);
            case IfNode(
                    ExpressionNode cond, List<StatementNode> thenBranch, List<StatementNode> elseBranch, NodeContext _
            ) -> {
                System.out.println(pad + "if " + cond);
                for (StatementNode stmt : thenBranch) {
                    print(stmt, indent + 1);
                }
                if (!elseBranch.isEmpty()) {
                    System.out.println(pad + "else");
                    for (StatementNode stmt : elseBranch) {
                        print(stmt, indent + 1);
                    }
                }
            }
            case LoopNode(
                    StatementNode init, ExpressionNode cond, StatementNode step, List<StatementNode> body,
                    NodeContext _
            ) -> {
                System.out.println(pad + "loop {");
                print(init, indent + 1);
                System.out.println(pad + "  cond: " + cond);
                System.out.println(pad + "  step: " + step);
                System.out.println(pad + "  body {");
                for (StatementNode stmt : body) {
                    print(stmt, indent + 2);
                }
                System.out.println(pad + "  }");
                System.out.println(pad + "}");
            }
            case ReturnNode r -> System.out.println(pad + "return" +
                    r.getExpression().map(e -> " " + e).orElse(""));
            case BreakNode _ -> System.out.println(pad + "break");
            case ExpressionStatementNode(ExpressionNode expression, NodeContext _) ->
                    System.out.println(pad + expression);
            case null, default -> System.out.println(pad + n);  // 回退处理
        }
    }

    /**
     * 打印 AST 的 JSON 表示形式。
     *
     * <p>
     * 本方法将语法树序列化为 JSON 字符串，并通过 {@link JsonFormatter#prettyPrint(String)} 方法进行格式化。
     * 适用于将 AST 结构导出至文件或供其他系统消费。
     * </p>
     *
     * @param nodes 要序列化并打印的 AST 节点列表。
     */
    public static void printJson(List<Node> nodes) {
        String rawJson = ASTJsonSerializer.toJsonString(nodes);
        String pretty = JsonFormatter.prettyPrint(rawJson);
        System.out.println(pretty);
    }
}
