package org.jcnc.snow.compiler.ir.value;

import org.jcnc.snow.compiler.ir.core.IRValue;

/**
 * IRLabel —— 表示中间表示（IR）系统中的跳转目标标签。
 * <p>
 * 标签用于控制流指令（如 JUMP等）中，
 * 作为程序执行跳转的目的地，是 IR 控制流图（CFG）中的基本构建块。
 * <p>
 * 每个标签由一个唯一的名称（String name）标识，
 * 可用于生成目标代码中的符号标签或跳转地址。
 * <p>
 * 该类实现了 {@link IRValue} 接口，因此也可被视为指令操作数，
 * 在某些 IRInstruction 中以参数形式出现（如条件跳转目标）。
 */
public record IRLabel(String name) implements IRValue {

    /**
     * 返回标签的字符串形式，便于打印或调试。
     * 通常表示为带冒号的形式，例如 "L1:"。
     *
     * @return 格式化后的标签字符串
     */
    @Override
    public String toString() {
        return name + ":";
    }
}
