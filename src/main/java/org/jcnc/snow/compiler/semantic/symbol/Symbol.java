package org.jcnc.snow.compiler.semantic.symbol;

import org.jcnc.snow.compiler.semantic.type.Type;

/**
 * {@code Symbol} 表示符号表中的一条符号记录，描述语言中命名实体的语义信息。
 * <p>
 * 符号是语义分析中的基础单元，通常用于表示变量、函数、参数等具名元素。
 * 每个符号具备以下三个核心属性:
 * <ul>
 *   <li><b>name</b>: 符号的名称，例如变量名或函数名；</li>
 *   <li><b>type</b>: 符号的类型信息，通常为 {@link Type} 的子类实例；</li>
 *   <li><b>kind</b>: 符号的种类，由 {@link SymbolKind} 枚举表示（例如 VARIABLE、FUNCTION 等）。</li>
 * <ul>
 *   <li>构造器 {@code Symbol(String, Type, SymbolKind)}；</li>
 *   <li>访问器方法 {@code name()}, {@code type()}, {@code kind()}；</li>
 *   <li>标准的 {@code equals()}、{@code hashCode()} 和 {@code toString()} 实现。</li>
 * </ul>
 *
 * @param name 符号名称，必须唯一且非空
 * @param type 符号所代表的类型，可为基础类型、函数类型等
 * @param kind 符号种类，决定其语义用途（例如变量、函数、参数等）
 */
public record Symbol(String name, Type type, SymbolKind kind) {
}
