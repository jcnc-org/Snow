package org.jcnc.snow.compiler.parser.utils;

import org.jcnc.snow.compiler.parser.ast.*;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.Node;

import java.util.*;

/**
 * {@code ASTJsonSerializer} 是抽象语法树（AST）序列化工具类。
 * <p>
 * 该工具可将编译器内部构建的 AST 节点对象转换为通用的 {@code Map} 和 {@code List} 结构，
 * 并可借助 {@code JSONParser.toJson(Object)} 方法将其序列化为 JSON 字符串，用于调试、
 * 可视化或跨语言数据传输。
 * <p>
 * 支持的节点类型包括（新增对 {@code BoolLiteralNode}、{@code UnaryExpressionNode} 的完整支持）：
 * <ul>
 *   <li>{@link ModuleNode}</li>
 *   <li>{@link FunctionNode}</li>
 *   <li>{@link DeclarationNode}</li>
 *   <li>{@link AssignmentNode}</li>
 *   <li>{@link IfNode}</li>
 *   <li>{@link LoopNode}</li>
 *   <li>{@link ReturnNode}</li>
 *   <li>{@link ExpressionStatementNode}</li>
 *   <li>{@link BoolLiteralNode}</li>
 *   <li>{@link UnaryExpressionNode}</li>
 *   <li>以及各类 {@link ExpressionNode} 子类型，如 {@code BinaryExpressionNode}, {@code IdentifierNode} 等</li>
 * </ul>
 */
public class ASTJsonSerializer {

    /**
     * 创建包含 {@code type} 字段的节点 Map，用于标识节点类型。
     *
     * @param type 节点类型字符串。
     * @return 一个初始化后的 Map 实例。
     */
    private static Map<String, Object> newNodeMap(String type) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        return m;
    }

    /**
     * 构建表达式节点的 Map 表示，支持动态键值对传参。
     *
     * @param type 表达式类型。
     * @param kv   可变参数（key-value 键值对）。
     * @return 表示表达式节点的 Map。
     */
    private static Map<String, Object> exprMap(String type, Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    /**
     * 将 AST 根节点列表转换为 JSON 字符串。
     *
     * @param ast 表示顶层语法树结构的节点列表。
     * @return 对应的 JSON 字符串表示形式。
     */
    public static String toJsonString(List<Node> ast) {
        List<Object> list = new ArrayList<>(ast.size());
        for (Node n : ast) {
            list.add(nodeToMap(n));
        }
        return JSONParser.toJson(list);
    }

    /**
     * 将任意 AST 节点递归转换为 Map/List 结构。
     *
     * @param n 要转换的 AST 节点。
     * @return 以 Map/List 表示的结构化数据。
     */
    private static Object nodeToMap(Node n) {
        return switch (n) {
            // 模块节点
            case ModuleNode(String name, List<ImportNode> imports, List<FunctionNode> functions) -> {
                Map<String, Object> map = newNodeMap("Module");
                map.put("name", name);
                List<Object> imps = new ArrayList<>(imports.size());
                for (ImportNode imp : imports) {
                    imps.add(Map.of("type", "Import", "module", imp.moduleName()));
                }
                map.put("imports", imps);
                List<Object> funcs = new ArrayList<>(functions.size());
                for (FunctionNode f : functions) {
                    funcs.add(nodeToMap(f));
                }
                map.put("functions", funcs);
                yield map;
            }
            // 函数定义节点
            case FunctionNode f -> {
                Map<String, Object> map = newNodeMap("Function");
                map.put("name", f.name());
                List<Object> params = new ArrayList<>(f.parameters().size());
                for (var p : f.parameters()) {
                    params.add(Map.of("name", p.name(), "type", p.type()));
                }
                map.put("parameters", params);
                map.put("returnType", f.returnType());
                List<Object> body = new ArrayList<>(f.body().size());
                for (Node stmt : f.body()) {
                    body.add(nodeToMap(stmt));
                }
                map.put("body", body);
                yield map;
            }
            // 变量声明节点
            case DeclarationNode d -> {
                Map<String, Object> map = newNodeMap("Declaration");
                map.put("name", d.getName());
                map.put("varType", d.getType());
                map.put("initializer", d.getInitializer().map(ASTJsonSerializer::exprToMap).orElse(null));
                yield map;
            }
            // 赋值语句节点
            case AssignmentNode a -> exprMap("Assignment",
                    "variable", a.variable(),
                    "value", exprToMap(a.value())
            );
            // 条件语句节点
            case IfNode i -> {
                Map<String, Object> map = newNodeMap("If");
                map.put("condition", exprToMap(i.condition()));
                List<Object> thenList = new ArrayList<>(i.thenBranch().size());
                for (Node stmt : i.thenBranch()) thenList.add(nodeToMap(stmt));
                map.put("then", thenList);
                if (!i.elseBranch().isEmpty()) {
                    List<Object> elseList = new ArrayList<>(i.elseBranch().size());
                    for (Node stmt : i.elseBranch()) elseList.add(nodeToMap(stmt));
                    map.put("else", elseList);
                }
                yield map;
            }
            // 循环语句节点
            case LoopNode l -> {
                Map<String, Object> map = newNodeMap("Loop");
                map.put("initializer", l.initializer() != null ? nodeToMap(l.initializer()) : null);
                map.put("condition", l.condition() != null ? exprToMap(l.condition()) : null);
                map.put("update", l.update() != null ? nodeToMap(l.update()) : null);
                List<Object> body = new ArrayList<>(l.body().size());
                for (Node stmt : l.body()) body.add(nodeToMap(stmt));
                map.put("body", body);
                yield map;
            }
            // return 语句节点
            case ReturnNode r -> {
                Map<String, Object> map = newNodeMap("Return");
                r.getExpression().ifPresent(expr -> map.put("value", exprToMap(expr)));
                yield map;
            }
            // 表达式语句节点
            case ExpressionStatementNode e -> exprMap("ExpressionStatement",
                    "expression", exprToMap(e.expression())
            );
            // 通用表达式节点
            case ExpressionNode expressionNode -> exprToMap(expressionNode);
            // 其他类型（兜底处理）
            default -> Map.of("type", n.getClass().getSimpleName());
        };
    }

    /**
     * 将表达式类型节点转换为 Map 表示形式。
     *
     * @param expr 表达式 AST 节点。
     * @return 表示该表达式的 Map。
     */
    private static Object exprToMap(ExpressionNode expr) {
        return switch (expr) {
            // 二元表达式
            case BinaryExpressionNode(ExpressionNode left, String operator, ExpressionNode right) -> exprMap("BinaryExpression",
                    "left", exprToMap(left),
                    "operator", operator,
                    "right", exprToMap(right)
            );
            // 一元表达式
            case UnaryExpressionNode(String operator, ExpressionNode operand) -> exprMap("UnaryExpression",
                    "operator", operator,
                    "operand", exprToMap(operand)
            );
            // 布尔字面量
            case BoolLiteralNode(boolean value) -> exprMap("BoolLiteral", "value", value);
            // 标识符
            case IdentifierNode(String name) -> exprMap("Identifier", "name", name);
            // 数字字面量
            case NumberLiteralNode(String value) -> exprMap("NumberLiteral", "value", value);
            // 字符串字面量
            case StringLiteralNode(String value) -> exprMap("StringLiteral", "value", value);
            // 调用表达式
            case CallExpressionNode(ExpressionNode callee, List<ExpressionNode> arguments, int line, int column, String file) -> {
                List<Object> args = new ArrayList<>(arguments.size());
                for (ExpressionNode arg : arguments) args.add(exprToMap(arg));
                yield exprMap("CallExpression", "callee", exprToMap(callee), "arguments", args);
            }
            // 成员访问表达式
            case MemberExpressionNode(ExpressionNode object, String member) -> exprMap("MemberExpression",
                    "object", exprToMap(object),
                    "member", member
            );
            // 默认兜底处理：只写类型
            default -> Map.of("type", expr.getClass().getSimpleName());
        };
    }
}
