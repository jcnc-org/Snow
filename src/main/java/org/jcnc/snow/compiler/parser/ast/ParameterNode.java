package org.jcnc.snow.compiler.parser.ast;

import org.jcnc.snow.compiler.parser.ast.base.Node;

/**
 * {@code ParameterNode} 表示抽象语法树（AST）中的函数参数定义节点。
 * <p>
 * 每个参数节点包含参数的名称和类型信息，
 * 用于构成函数签名并参与类型检查与函数调用匹配。
 * </p>
 *
 * @param name 参数名称标识符
 * @param type 参数类型字符串（如 "int"、"string"）
 */
public record ParameterNode(String name, String type) implements Node {

    /**
     * 返回参数的字符串形式，格式为 {@code name:type}。
     * <p>
     * 用于调试输出或构建函数签名描述。
     * </p>
     *
     * @return 参数的字符串形式（如 {@code count:int}）
     */
    @Override
    public String toString() {
        return name + ":" + type;
    }
}