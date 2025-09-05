package org.jcnc.snow.compiler.ir.builder.expression;

import org.jcnc.snow.compiler.ir.builder.core.InstructionFactory;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;

/**
 * 表达式处理器接口。
 * <p>
 * 每种 ExpressionNode（表达式 AST 节点）类型都应实现一个对应的 ExpressionHandler，
 * 实现具体的表达式到 IR 指令的转换逻辑。
 *
 * @param <T> 具体处理的 ExpressionNode 子类型
 */
public interface ExpressionHandler<T extends ExpressionNode> {
    /**
     * 处理表达式节点，将其转换为对应的 IR 指令，并返回结果寄存器。
     *
     * @param ctx  表达式构建器（用于访问 IR 上下文、分派子表达式等）
     * @param node 需要处理的表达式 AST 节点
     * @return 存放表达式结果的虚拟寄存器
     */
    IRVirtualRegister handle(ExpressionBuilder ctx, T node);

    /**
     * 默认实现：将表达式的结果写入指定目标寄存器。
     * <p>
     * 先调用 {@link #handle(ExpressionBuilder, ExpressionNode)} 获取结果，
     * 再通过 IR 指令将结果移动到目标寄存器 dest。
     * 绝大多数场景无需重写，特殊处理可覆盖。
     *
     * @param ctx  表达式构建器
     * @param node 表达式 AST 节点
     * @param dest 目标虚拟寄存器
     */
    default void handleInto(ExpressionBuilder ctx, T node, IRVirtualRegister dest) {
        IRVirtualRegister tmp = handle(ctx, node);
        InstructionFactory.move(ctx.ctx(), tmp, dest);
    }
}
