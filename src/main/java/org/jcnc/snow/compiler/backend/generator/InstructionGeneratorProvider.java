package org.jcnc.snow.compiler.backend.generator;

import org.jcnc.snow.compiler.backend.core.InstructionGenerator;
import org.jcnc.snow.compiler.ir.core.IRInstruction;

import java.util.List;

/**
 * 指令生成器集中注册表。
 *
 * <p>本类在类加载阶段即完成所有后端 {@link InstructionGenerator} 实例
 * 的创建，并以不可变列表形式对外暴露。
 */
public final class InstructionGeneratorProvider {

    /**
     * 缺省指令生成器列表（不可修改，顺序即执行顺序）。
     */
    private static final List<InstructionGenerator<? extends IRInstruction>> DEFAULT =
            List.of(
                    new LoadConstGenerator(),  // 常量加载
                    new BinaryOpGenerator(),   // 二元运算
                    new UnaryOpGenerator(),    // 一元运算
                    new CallGenerator(),       // 函数调用
                    new ReturnGenerator(),     // 函数返回
                    new LabelGenerator(),      // 标签定义
                    new JumpGenerator(),       // 无条件跳转
                    new CmpJumpGenerator()     // 条件跳转
            );

    /**
     * 工具类禁止实例化。
     */
    private InstructionGeneratorProvider() { /* no-instance */ }

    /**
     * 返回生产环境使用的缺省指令生成器列表。
     * 该列表为不可变集合，如尝试修改将抛出
     * {@link UnsupportedOperationException}。
     *
     * @return 不可变的 {@code List} 实例
     */
    public static List<InstructionGenerator<? extends IRInstruction>> defaultGenerators() {
        return DEFAULT;
    }
}
