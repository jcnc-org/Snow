package org.jcnc.snow.compiler.parser.ast;

import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;

import java.util.List;

/**
 * {@code NewExpressionNode}
 * <p>
 * 抽象语法树（AST）节点 —— 对象创建表达式。
 * <pre>new TypeName(arg1, arg2, ...)</pre>
 *
 * <p>
 * 语义：在源码中通过 <b>new</b> 关键字实例化某个类型，
 * 实参列表为构造函数参数。表达式类型为对应结构体类型。
 * </p>
 *
 * <p>
 * 字段说明：
 * <ul>
 *   <li>{@code typeName} —— 创建对象的目标类型名（如 "Person"、"MyStruct"）。</li>
 *   <li>{@code arguments} —— 构造函数参数表达式列表（可为空）。</li>
 *   <li>{@code ctx} —— 源码位置上下文（行列信息，便于报错和调试定位）。</li>
 * </ul>
 * </p>
 *
 * @param typeName  目标类型名
 * @param arguments 构造参数表达式列表
 * @param ctx       源代码位置信息
 */
public record NewExpressionNode(
        String typeName,
        List<ExpressionNode> arguments,
        NodeContext ctx
) implements ExpressionNode {

    /**
     * 获取本节点的源代码位置信息（行号、列号等）。
     * <p>
     * 实现 ExpressionNode 接口的抽象方法，便于统一错误处理与调试。
     * </p>
     *
     * @return 当前节点的源代码上下文信息
     */
    @Override
    public NodeContext context() {
        return ctx;
    }
}
