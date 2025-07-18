package org.jcnc.snow.compiler.semantic.core;

import org.jcnc.snow.compiler.semantic.type.*;

import java.util.Map;

/**
 * {@code BuiltinTypeRegistry} 是内置类型和内置模块的集中注册中心。
 * <p>
 * 本类主要负责: 
 * <ul>
 *   <li>定义语言中所有可识别的基础类型（如 int、float、string 等）；</li>
 *   <li>在语义分析初始化时，将内置模块（如 {@code BuiltinUtils}）注册到上下文中；</li>
 *   <li>提供对内置类型的快速查找支持。</li>
 * </ul>
 * 该类为纯工具类，所有成员均为静态，不可实例化。
 */
public final class BuiltinTypeRegistry {

    /**
     * 内置类型映射表: 将类型名称字符串映射到对应的 {@link Type} 实例。
     * <p>
     * 用于类型解析过程（如解析变量声明或函数返回类型）中，
     * 将用户源码中的类型字符串转换为语义类型对象。
     */
    public static final Map<String, Type> BUILTIN_TYPES = Map.of(
            "int",    BuiltinType.INT,
            "long",   BuiltinType.LONG,
            "short",  BuiltinType.SHORT,
            "byte",   BuiltinType.BYTE,
            "float",  BuiltinType.FLOAT,
            "double", BuiltinType.DOUBLE,
            "string", BuiltinType.STRING,
            "boolean", BuiltinType.BOOLEAN,
            "void",   BuiltinType.VOID
    );

    /**
     * 私有构造函数，禁止实例化。
     */
    private BuiltinTypeRegistry() { }

    /**
     * 初始化语义上下文中与内置模块相关的内容。
     * <p>
     * 当前实现将内置模块 {@code BuiltinUtils} 注册至上下文模块表中，
     * 使其在用户代码中可被访问（如 {@code BuiltinUtils.to_string(...)}）。
     *
     * @param ctx 当前语义分析上下文
     */
    public static void init(Context ctx) {

    }
}
