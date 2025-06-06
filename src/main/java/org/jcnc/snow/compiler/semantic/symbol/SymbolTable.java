package org.jcnc.snow.compiler.semantic.symbol;

import java.util.HashMap;
import java.util.Map;

/**
 * {@code SymbolTable} 表示一个语义作用域，用于管理命名实体（如变量、函数、模块等）的符号信息。
 * <p>
 * 本类支持嵌套作用域结构（Nested Scope），适用于块级作用域、函数作用域、模块作用域等语义环境建模。
 * 每个符号表可挂接一个“父作用域”，以支持多层级作用域链上的符号解析。
 *
 * <p>核心特性包括：
 * <ul>
 *   <li>符号定义（局部作用域内唯一）；</li>
 *   <li>符号查找（向上查找父作用域）；</li>
 *   <li>嵌套作用域支持（通过 parent 引用）；</li>
 *   <li>可用于语义分析、类型检查、作用域验证等场景。</li>
 * </ul>
 */
public class SymbolTable {

    /** 父作用域引用，若为 {@code null} 则表示为最外层作用域（全局或根作用域） */
    private final SymbolTable parent;

    /** 当前作用域内定义的符号映射表（符号名 → {@link Symbol}） */
    private final Map<String, Symbol> symbols = new HashMap<>();

    /**
     * 创建一个新的符号表实例。
     *
     * @param parent 父作用域符号表，若无父作用域则传入 {@code null}
     */
    public SymbolTable(SymbolTable parent) {
        this.parent = parent;
    }

    /**
     * 在当前作用域中定义一个新的符号。
     * <p>
     * 若符号名称在当前作用域中已存在，则定义失败并返回 {@code false}；
     * 否则将该符号添加至当前符号映射中并返回 {@code true}。
     *
     * @param symbol 待定义的符号实体
     * @return 若定义成功则返回 {@code true}；否则返回 {@code false}
     */
    public boolean define(Symbol symbol) {
        if (symbols.containsKey(symbol.name())) {
            return false;
        }
        symbols.put(symbol.name(), symbol);
        return true;
    }

    /**
     * 根据名称解析符号，支持作用域链向上递归查找。
     * <p>查找策略：
     * <ol>
     *   <li>优先在当前作用域中查找该符号名称；</li>
     *   <li>若未找到且存在父作用域，则递归向上查找；</li>
     *   <li>若所有作用域中均未找到，则返回 {@code null}。</li>
     * </ol>
     *
     * @param name 要解析的符号名称
     * @return 对应的 {@link Symbol} 实例，若未找到则返回 {@code null}
     */
    public Symbol resolve(String name) {
        Symbol sym = symbols.get(name);
        if (sym != null) {
            return sym;
        }
        return (parent != null) ? parent.resolve(name) : null;
    }
}
