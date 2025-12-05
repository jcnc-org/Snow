package org.jcnc.snow.compiler.ir.core;

import org.jcnc.snow.compiler.ir.value.IRConstant;
import org.jcnc.snow.compiler.ir.value.IRLabel;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

/**
 * {@code IRValue} 表示 IR 指令系统中可被操作的值类型。
 * <p>
 * 它定义了所有 IR 指令在使用操作数、参数、结果或跳转目标时的统一抽象。
 * 实现该接口的类型可以作为 {@link IRInstruction} 中的操作数出现。
 * </p>
 *
 * <p>当前支持的 IR 值类型包括: </p>
 * <ul>
 *     <li>{@link IRVirtualRegister}: 虚拟寄存器，表示计算结果或中间变量</li>
 *     <li>{@link IRConstant}: 常量值，表示不可变的字面量或数值</li>
 *     <li>{@link IRLabel}: 标签，表示跳转指令的目标地址</li>
 * </ul>
 *
 * <p>
 * 该接口声明为 {@code sealed interface}，限制只能被上述类型实现。
 * 这种设计允许编译器对 {@code IRValue} 的使用进行静态穷尽性检查，
 * 有助于提升类型安全性与维护性。
 * </p>
 */
public interface IRValue {
}