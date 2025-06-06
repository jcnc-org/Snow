package org.jcnc.snow.compiler.ir.value;

import org.jcnc.snow.compiler.ir.core.IRValue;

/**
 * IRConstant —— 表示中间表示（IR）系统中的常量值。
 * <p>
 * 常量用于表示在编译期间已知的不可变值，例如字面整数、浮点数、布尔值或字符串。
 * 与 {@link IRVirtualRegister} 不同，常量不需要通过寄存器存储，
 * 可直接作为 IR 指令的操作数使用。
 * <p>
 * 典型应用：
 * - 加载常量指令：v1 = CONST 42
 * - 计算表达式：v2 = ADD v1, 100
 */
public record IRConstant(Object value) implements IRValue {

    /**
     * 将常量值转换为字符串，用于打印 IR 指令或调试输出。
     * <p>
     * 例如：
     * - 整数常量：42
     * - 字符串常量："hello"
     *
     * @return 常量的字符串表示
     */
    @Override
    public String toString() {
        return value.toString();
    }
}
