package org.jcnc.snow.compiler.ir.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@code IRProgram} 表示一份完整的中间表示（Intermediate Representation, IR）程序。
 * <p>
 * 它作为编译器后端处理阶段的核心结构，承载所有由源代码翻译得到的 {@link IRFunction} 实例，
 * 形成整体性的中间表示单元，便于进行后续的优化、目标代码生成或静态分析。
 * </p>
 */
public final class IRProgram {

    /**
     * 存储程序中所有函数的有序集合。
     */
    private final List<IRFunction> functions = new ArrayList<>();

    /**
     * 将一个 {@link IRFunction} 添加到程序中。
     * <p>
     * 函数会按添加顺序保留在内部集合中。
     * </p>
     *
     * @param irFunction 要加入的 IR 函数对象
     */
    public void add(IRFunction irFunction) {
        functions.add(irFunction);
    }

    /**
     * 获取程序中全部函数的只读视图。
     * <p>
     * 外部调用者无法通过返回的列表修改函数集合，从而确保封装性与结构完整性。
     * </p>
     *
     * @return 不可变的函数列表
     */
    public List<IRFunction> functions() {
        return Collections.unmodifiableList(functions);
    }

    /**
     * 返回该 IR 程序的字符串形式。
     * <p>
     * 每个函数按其 {@code toString()} 表示输出，换行分隔。
     * 通常用于调试与日志输出。
     * </p>
     *
     * @return 表示整个 IR 程序的格式化字符串
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (IRFunction f : functions) {
            sb.append(f).append('\n');
        }
        return sb.toString();
    }
}
