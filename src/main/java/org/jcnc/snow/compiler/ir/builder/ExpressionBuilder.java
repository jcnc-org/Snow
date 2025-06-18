package org.jcnc.snow.compiler.ir.builder;

import org.jcnc.snow.compiler.ir.core.IROpCode;
import org.jcnc.snow.compiler.ir.instruction.CallInstruction;
import org.jcnc.snow.compiler.ir.instruction.LoadConstInstruction;
import org.jcnc.snow.compiler.ir.instruction.UnaryOperationInstruction;
import org.jcnc.snow.compiler.ir.value.IRConstant;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.compiler.ir.utils.ExpressionUtils;
import org.jcnc.snow.compiler.parser.ast.*;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;

import java.util.*;

/**
 * <b>表达式构建器</b>
 * <p>
 * 该类负责将抽象语法树（AST）的表达式节点转换为中间表示（IR）指令和虚拟寄存器，
 * 是编译器IR生成阶段的核心工具。
 * <br/>
 * 主要职责包括：
 * <ul>
 *   <li>将数字字面量、标识符、二元表达式、函数调用等AST表达式节点，翻译为对应的IR指令序列</li>
 *   <li>管理并分配虚拟寄存器，保证IR操作的数据流正确</li>
 * </ul>
 * <p>
 */
public record ExpressionBuilder(IRContext ctx) {

    /**
     * 构建并返回某个表达式节点对应的虚拟寄存器。
     *
     * <p>会根据节点的实际类型分别处理：
     * <ul>
     *   <li>数字字面量：新建常量寄存器</li>
     *   <li>布尔字面量：生成值为 0 或 1 的常量寄存器</li>
     *   <li>标识符：查找当前作用域中的寄存器</li>
     *   <li>二元表达式：递归处理子表达式并进行相应运算</li>
     *   <li>一元运算符：
     *     <ul>
     *        <li><code>-x</code>（取负，生成 <code>NEG_I32</code> 指令）与</li>
     *        <li>code>!x</code>（逻辑非，转换为 <code>x == 0</code> 比较指令）</li>
     *     </ul>
     *   </li>
     *   <li>函数调用：生成对应的Call指令</li>
     *   <li>其它类型不支持，抛出异常</li>
     * </ul>
     *
     * @param expr 要转换的表达式AST节点
     * @return 该表达式的计算结果寄存器
     * @throws IllegalStateException 如果遇到未定义的标识符或不支持的表达式类型
     */

    public IRVirtualRegister build(ExpressionNode expr) {
        return switch (expr) {
            // 数字字面量
            case NumberLiteralNode n -> buildNumberLiteral(n.value());
            // 布尔字面量
            case BoolLiteralNode b   -> buildBoolLiteral(b.getValue());
            // 标识符
            case IdentifierNode id -> {
                IRVirtualRegister reg = ctx.getScope().lookup(id.name());
                if (reg == null)
                    throw new IllegalStateException("未定义标识符: " + id.name());
                yield reg;
            }
            // 二元表达式
            case BinaryExpressionNode bin -> buildBinary(bin);
            // 函数调用
            case CallExpressionNode call -> buildCall(call);
            case UnaryExpressionNode u  -> buildUnary(u);
            default -> throw new IllegalStateException(
                    "不支持的表达式类型: " + expr.getClass().getSimpleName());
        };
    }

    /** 处理一元表达式 */
    private IRVirtualRegister buildUnary(UnaryExpressionNode un) {
        String            op  = un.operator();
        IRVirtualRegister val = build(un.operand());

        //  -x  → NEG_*（根据类型自动选择位宽）
        if (op.equals("-")) {
            IRVirtualRegister dest = ctx.newRegister();
            IROpCode          code = ExpressionUtils.negOp(un.operand());
            ctx.addInstruction(new UnaryOperationInstruction(code, dest, val));
            return dest;
        }

        //  !x  →  (x == 0)
        if (op.equals("!")) {
            IRVirtualRegister zero = InstructionFactory.loadConst(ctx, 0);
            return InstructionFactory.binOp(ctx, IROpCode.CMP_EQ, val, zero);
        }

        throw new IllegalStateException("未知一元运算符: " + op);
    }

    /**
     * 直接将表达式计算结果写入指定的目标寄存器（dest）。
     * <p>
     * 与{@link #build(ExpressionNode)}类似，但支持目标寄存器复用（避免不必要的move）。
     *
     * @param node 表达式AST节点
     * @param dest 目标寄存器
     * @throws IllegalStateException 未定义标识符/不支持的表达式类型时报错
     */
    public void buildInto(ExpressionNode node, IRVirtualRegister dest) {
        switch (node) {
            // 数字字面量，直接加载到目标寄存器
            case NumberLiteralNode n ->
                    InstructionFactory.loadConstInto(ctx, dest, ExpressionUtils.buildNumberConstant(ctx, n.value()));
            // 标识符，查找并move到目标寄存器
            case IdentifierNode id -> {
                IRVirtualRegister src = ctx.getScope().lookup(id.name());
                if (src == null) throw new IllegalStateException("未定义标识符: " + id.name());
                InstructionFactory.move(ctx, src, dest);
            }
            // 二元表达式，直接写入目标寄存器
            case BinaryExpressionNode bin -> buildBinaryInto(bin, dest);
            // 其他表达式，先递归生成寄存器，再move到目标寄存器
            default -> {
                IRVirtualRegister tmp = build(node);
                InstructionFactory.move(ctx, tmp, dest);
            }
        }
    }

    /**
     * 构建二元表达式的IR，生成新寄存器存储结果。
     * <p>
     * 先递归构建左右操作数，之后根据操作符类别（算术或比较）决定生成的IR操作码，
     * 并生成对应的二元运算指令。
     *
     * @param bin 二元表达式节点
     * @return 存放结果的虚拟寄存器
     */
    private IRVirtualRegister buildBinary(BinaryExpressionNode bin) {
        String op = bin.operator();
        IRVirtualRegister left = build(bin.left());
        IRVirtualRegister right = build(bin.right());
        // 处理比较操作符
        if (ExpressionUtils.isComparisonOperator(op)) {
            return InstructionFactory.binOp(ctx, ExpressionUtils.cmpOp(op), left, right);
        }
        // 处理算术运算符
        IROpCode code = ExpressionUtils.resolveOpCode(op, bin.left(), bin.right());
        if (code == null) throw new IllegalStateException("不支持的运算符: " + op);
        return InstructionFactory.binOp(ctx, code, left, right);
    }

    /**
     * 将二元表达式的结果直接写入指定寄存器dest。
     * <p>
     * 结构与{@link #buildBinary(BinaryExpressionNode)}类似，但不会新分配寄存器。
     *
     * @param bin 二元表达式节点
     * @param dest 目标寄存器
     */
    private void buildBinaryInto(BinaryExpressionNode bin, IRVirtualRegister dest) {
        IRVirtualRegister a = build(bin.left());
        IRVirtualRegister b = build(bin.right());
        String op = bin.operator();
        if (ExpressionUtils.isComparisonOperator(op)) {
            InstructionFactory.binOpInto(ctx, ExpressionUtils.cmpOp(op), a, b, dest);
        } else {
            IROpCode code = ExpressionUtils.resolveOpCode(op, bin.left(), bin.right());
            if (code == null) throw new IllegalStateException("不支持的运算符: " + op);
            InstructionFactory.binOpInto(ctx, code, a, b, dest);
        }
    }

    /**
     * 处理函数调用表达式，生成对应的Call指令和目标寄存器。
     * <p>
     * 支持普通标识符调用和成员调用（如 mod.func），会为每个参数依次生成子表达式的寄存器。
     *
     * @param call 调用表达式AST节点
     * @return 返回结果存放的寄存器
     */
    private IRVirtualRegister buildCall(CallExpressionNode call) {
        // 递归构建所有参数的寄存器
        List<IRVirtualRegister> argv = call.arguments().stream()
                .map(this::build)
                .toList();
        // 获取完整调用目标名称（支持成员/模块调用和普通调用）
        String fullName = switch (call.callee()) {
            case MemberExpressionNode member when member.object() instanceof IdentifierNode _ ->
                    ((IdentifierNode)member.object()).name() + "." + member.member();
            case IdentifierNode id -> id.name();
            default -> throw new IllegalStateException("不支持的调用目标: " + call.callee().getClass().getSimpleName());
        };
        // 申请目标寄存器
        IRVirtualRegister dest = ctx.newRegister();
        // 添加Call指令到IR上下文
        ctx.addInstruction(new CallInstruction(dest, fullName, new ArrayList<>(argv)));
        return dest;
    }

    /**
     * 处理数字字面量，生成常量寄存器和加载指令。
     * <p>
     * 会将字符串型字面量（如 "123", "1.0f"）解析为具体的IRConstant，
     * 并分配一个新的虚拟寄存器来存放该常量。
     *
     * @param value 字面量字符串
     * @return 存放该常量的寄存器
     */
    private IRVirtualRegister buildNumberLiteral(String value) {
        IRConstant constant = ExpressionUtils.buildNumberConstant(ctx, value);
        IRVirtualRegister reg = ctx.newRegister();
        ctx.addInstruction(new LoadConstInstruction(reg, constant));
        return reg;
    }

    /** 布尔字面量 → CONST （true=1，false=0）*/
    private IRVirtualRegister buildBoolLiteral(boolean value) {
        IRConstant constant = new IRConstant(value ? 1 : 0);
        IRVirtualRegister reg = ctx.newRegister();
        ctx.addInstruction(new LoadConstInstruction(reg, constant));
        return reg;
    }
}
