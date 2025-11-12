package org.jcnc.snow.compiler.ir.builder.handlers;

import org.jcnc.snow.compiler.ir.builder.core.InstructionFactory;
import org.jcnc.snow.compiler.ir.builder.expression.ExpressionBuilder;
import org.jcnc.snow.compiler.ir.builder.expression.ExpressionHandler;
import org.jcnc.snow.compiler.ir.core.IROpCode;
import org.jcnc.snow.compiler.ir.instruction.UnaryOperationInstruction;
import org.jcnc.snow.compiler.ir.utils.ExpressionUtils;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.compiler.parser.ast.UnaryExpressionNode;

import java.util.Locale;

/**
 * 一元运算表达式处理器。
 * <p>
 * 该类负责将一元运算表达式（如 {@code -x}、{@code !x}）转换为对应的 IR 指令。
 * <p>
 * 支持的操作包括：
 * <ul>
 *     <li>算术负号（{@code -x}）</li>
 *     <li>逻辑非（{@code !x}）</li>
 * </ul>
 * <p>
 * 该处理器会根据操作数的寄存器类型选择合适的 {@link IROpCode}，
 * 并自动维护寄存器类型信息。
 */
public class UnaryHandler implements ExpressionHandler<UnaryExpressionNode> {

    /**
     * 处理一元表达式节点，生成对应的 IR 指令。
     * <p>
     * 处理逻辑如下：
     * <ol>
     *   <li>递归构建操作数表达式，获得源寄存器；</li>
     *   <li>分配新的目标寄存器；</li>
     *   <li>根据操作符类型生成不同的指令：
     *       <ul>
     *         <li>{@code -x}：选择合适的 NEG 操作码生成 {@link UnaryOperationInstruction}；</li>
     *         <li>{@code !x}：通过比较 {@code x == 0} 实现逻辑非操作。</li>
     *       </ul>
     *   </li>
     *   <li>推断并传播寄存器类型。</li>
     * </ol>
     *
     * @param b  表达式构建器，用于访问当前 {@link org.jcnc.snow.compiler.ir.builder.core.IRContext}
     * @param un 一元表达式 AST 节点
     * @return 存放一元表达式结果的虚拟寄存器
     * @throws IllegalStateException 当遇到未知操作符时抛出
     */
    @Override
    public IRVirtualRegister handle(ExpressionBuilder b, UnaryExpressionNode un) {
        // 1. 先构建操作数，得到其结果寄存器
        IRVirtualRegister src = b.build(un.operand());
        // 2. 分配目标寄存器
        IRVirtualRegister dest = b.ctx().newRegister();

        switch (un.operator()) {
            case "-" -> {
                // 算术负号：选择合适的 NEG 操作码
                IROpCode op = selectNegOpcode(b, un, src);
                b.ctx().addInstruction(new UnaryOperationInstruction(op, dest, src));
                propagateTypeFromOperand(b, src, dest);
            }
            case "!" -> {
                // 逻辑非：生成比较 x == 0 的表达式
                IRVirtualRegister zero = InstructionFactory.loadConst(b.ctx(), 0);
                return InstructionFactory.binOp(b.ctx(), IROpCode.CMP_IEQ, src, zero);
            }
            default ->
                // 不支持的操作符
                    throw new IllegalStateException("未知一元运算符: " + un.operator());
        }
        return dest;
    }

    /**
     * 根据操作数类型选择对应的取负（NEG）操作码。
     * <p>
     * 若寄存器类型可确定，则根据类型精确选择；
     * 否则调用 {@link ExpressionUtils 进行自动推断。
     * <p>
     * @param b   表达式构建器
     * @param un  一元表达式节点
     * @param src 源寄存器
     * @return 对应的 NEG 操作码
     */
    private IROpCode selectNegOpcode(ExpressionBuilder b, UnaryExpressionNode un, IRVirtualRegister src) {
        String type = b.ctx().getScope().getRegisterType(src);
        if (type != null) {
            return switch (type.toLowerCase(Locale.ROOT)) {
                case "byte" -> IROpCode.NEG_B8;
                case "short" -> IROpCode.NEG_S16;
                case "int" -> IROpCode.NEG_I32;
                case "long" -> IROpCode.NEG_L64;
                case "float" -> IROpCode.NEG_F32;
                case "double" -> IROpCode.NEG_D64;
                default -> ExpressionUtils.negOp(un.operand());
            };
        }
        return ExpressionUtils.negOp(un.operand());
    }

    /**
     * 将操作数寄存器的类型信息传播到目标寄存器。
     *
     * @param b    表达式构建器
     * @param src  源寄存器
     * @param dest 目标寄存器
     */
    private void propagateTypeFromOperand(ExpressionBuilder b, IRVirtualRegister src, IRVirtualRegister dest) {
        String type = b.ctx().getScope().getRegisterType(src);
        if (type != null) {
            b.ctx().getScope().setRegisterType(dest, type);
        }
    }
}
