package org.jcnc.snow.compiler.parser.ast;

import org.jcnc.snow.compiler.parser.ast.base.Node;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;

import java.util.List;
import java.util.StringJoiner;

/**
 * {@code StructNode}
 * <p>
 * AST 节点 —— 结构体定义（struct）。
 * </p>
 *
 * <p>
 * 描述一个结构体类型，包括字段、可选构造函数、方法列表等。
 * 结构体可声明零个或多个字段，可选构造函数（init），
 * 以及零个或多个方法。
 *
 * @param name    结构体名称
 * @param parent  父类名称（无继承时为 {@code null}）
 * @param fields  字段声明列表
 * @param init    构造函数（可为 null）
 * @param methods 方法列表
 * @param context 源码位置信息
 */
public record StructNode(
        String name,
        String parent,
        List<DeclarationNode> fields,
        FunctionNode init,
        List<FunctionNode> methods,
        NodeContext context
) implements Node {

    /**
     * 输出结构体节点的简明字符串表示，便于调试和日志查看。
     *
     * @return 结构体节点的简要信息，包括名称、字段、构造函数、方法
     */
    @Override
    public String toString() {
        // 1) 构造字段声明的字符串（类型+名称）
        StringJoiner fj = new StringJoiner(", ");
        fields.forEach(d -> fj.add(d.getType() + " " + d.getName()));

        // 2) 构造方法名列表字符串
        StringJoiner mj = new StringJoiner(", ");
        methods.forEach(f -> mj.add(f.name()));

        // 3) 合成最终输出
        return "Struct(name=" + name +
                ", parent=" + (parent == null ? "null" : parent) +
                ", fields=[" + fj + "], init=" +
                (init == null ? "null" : init.name()) +
                ", methods=[" + mj + "])";
    }
}
