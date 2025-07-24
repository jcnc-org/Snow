package org.jcnc.snow.compiler.ir.builder;

import org.jcnc.snow.compiler.ir.core.IROpCode;
import org.jcnc.snow.compiler.ir.instruction.CallInstruction;
import org.jcnc.snow.compiler.ir.instruction.LoadConstInstruction;
import org.jcnc.snow.compiler.ir.instruction.UnaryOperationInstruction;
import org.jcnc.snow.compiler.ir.utils.ComparisonUtils;
import org.jcnc.snow.compiler.ir.utils.ExpressionUtils;
import org.jcnc.snow.compiler.ir.value.IRConstant;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.compiler.parser.ast.*;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;

import java.util.*;

/**
 * <b>ExpressionBuilder - 表达式 → IR 构建器</b>
 *
 * <p>
 * 负责将 AST 表达式节点递归转换为 IR 虚拟寄存器操作，并生成对应的 IR 指令序列。
 * 支持字面量、标识符、二元表达式、一元表达式、函数调用等多种类型表达式。
 * </p>
 *
 * <p>
 * 主要功能：
 * <ul>
 *     <li>将表达式节点映射为虚拟寄存器</li>
 *     <li>为每种表达式类型生成对应 IR 指令</li>
 *     <li>支持表达式嵌套的递归构建</li>
 *     <li>支持写入指定目标寄存器，避免冗余的 move 指令</li>
 * </ul>
 * </p>
 */
public record ExpressionBuilder(IRContext ctx) {

    /* ───────────────── 顶层入口 ───────────────── */

    /**
     * 构建任意 AST 表达式节点，自动为其分配一个新的虚拟寄存器，并返回该寄存器。
     *
     * <p>
     * 这是表达式 IR 生成的核心入口。它会根据不同的表达式类型进行分派，递归构建 IR 指令。
     * </p>
     *
     * @param expr 任意 AST 表达式节点
     * @return 存储该表达式结果的虚拟寄存器
     * @throws IllegalStateException 遇到不支持的表达式类型或未定义标识符
     */
    public IRVirtualRegister build(ExpressionNode expr) {
        return switch (expr) {
            // 数字字面量，例如 123、3.14
            case NumberLiteralNode n -> buildNumberLiteral(n.value());
            // 字符串字面量，例如 "abc"
            case StringLiteralNode s -> buildStringLiteral(s.value());
            // 布尔字面量，例如 true / false
            case BoolLiteralNode b   -> buildBoolLiteral(b.getValue());
            // 标识符（变量名），如 a、b
            case IdentifierNode id -> {
                // 查找当前作用域中的变量寄存器
                IRVirtualRegister reg = ctx.getScope().lookup(id.name());
                if (reg == null)
                    throw new IllegalStateException("未定义标识符: " + id.name());
                yield reg;
            }
            // 二元表达式（如 a+b, x==y）
            case BinaryExpressionNode bin -> buildBinary(bin);
            // 函数/方法调用表达式
            case CallExpressionNode   call -> buildCall(call);
            // 一元表达式（如 -a, !a）
            case UnaryExpressionNode  un   -> buildUnary(un);

            // 默认分支：遇到未知表达式类型则直接抛异常
            default -> throw new IllegalStateException(
                    "不支持的表达式类型: " + expr.getClass().getSimpleName());
        };
    }

    /* ───────────────── 写入指定寄存器 ───────────────── */

    /**
     * 生成表达式，并将其结果直接写入目标寄存器，避免冗余的 move 操作。
     *
     * <p>
     * 某些简单表达式（如字面量、变量名）可以直接写入目标寄存器；复杂表达式则会先 build 到新寄存器，再 move 到目标寄存器。
     * </p>
     *
     * @param node 要生成的表达式节点
     * @param dest 目标虚拟寄存器（用于存储结果）
     */
    public void buildInto(ExpressionNode node, IRVirtualRegister dest) {
        switch (node) {
            // 数字字面量：生成 loadConst 指令写入目标寄存器
            case NumberLiteralNode n ->
                    InstructionFactory.loadConstInto(
                            ctx, dest, ExpressionUtils.buildNumberConstant(ctx, n.value()));

            // 字符串字面量：生成 loadConst 指令写入目标寄存器
            case StringLiteralNode s ->
                    InstructionFactory.loadConstInto(
                            ctx, dest, new IRConstant(s.value()));

            // 布尔字面量：转换为 int 1/0，生成 loadConst
            case BoolLiteralNode b ->
                    InstructionFactory.loadConstInto(
                            ctx, dest, new IRConstant(b.getValue() ? 1 : 0));

            // 标识符：查表获取原寄存器，然后 move 到目标寄存器
            case IdentifierNode id -> {
                IRVirtualRegister src = ctx.getScope().lookup(id.name());
                if (src == null)
                    throw new IllegalStateException("未定义标识符: " + id.name());
                InstructionFactory.move(ctx, src, dest);
            }

            // 二元表达式：递归生成并写入目标寄存器
            case BinaryExpressionNode bin -> buildBinaryInto(bin, dest);

            // 其它复杂情况：先 build 到新寄存器，再 move 到目标寄存器
            default -> {
                IRVirtualRegister tmp = build(node);
                InstructionFactory.move(ctx, tmp, dest);
            }
        }
    }

    /* ───────────────── 具体表达式类型 ───────────────── */

    /**
     * 一元表达式构建
     *
     * <p>
     * 支持算术取负（-a）、逻辑非（!a）等一元运算符。
     * </p>
     *
     * @param un 一元表达式节点
     * @return 结果存储的新分配虚拟寄存器
     */
    private IRVirtualRegister buildUnary(UnaryExpressionNode un) {
        // 递归生成操作数的寄存器
        IRVirtualRegister src  = build(un.operand());
        // 分配目标寄存器
        IRVirtualRegister dest = ctx.newRegister();

        switch (un.operator()) {
            // 算术负号：生成取负指令
            case "-" -> ctx.addInstruction(
                    new UnaryOperationInstruction(ExpressionUtils.negOp(un.operand()), dest, src));
            // 逻辑非：等价于 a == 0，生成比较指令
            case "!" -> {
                IRVirtualRegister zero = InstructionFactory.loadConst(ctx, 0);
                return InstructionFactory.binOp(ctx, IROpCode.CMP_IEQ, src, zero);
            }
            // 其它一元运算符不支持，抛异常
            default -> throw new IllegalStateException("未知一元运算符: " + un.operator());
        }
        return dest;
    }

    /**
     * 构建函数或方法调用表达式。
     *
     * @param call AST 调用表达式节点
     * @return 存储调用结果的虚拟寄存器
     */
    private IRVirtualRegister buildCall(CallExpressionNode call) {
        // 递归生成所有参数（实参）对应的寄存器
        List<IRVirtualRegister> argv = call.arguments().stream().map(this::build).toList();

        // 解析被调用目标名，支持普通函数/成员方法
        String callee = switch (call.callee()) {
            // 成员方法调用，例如 obj.foo()
            case MemberExpressionNode m when m.object() instanceof IdentifierNode id
                    -> id.name() + "." + m.member();
            // 普通函数调用
            case IdentifierNode id -> id.name();
            // 其它情况暂不支持
            default -> throw new IllegalStateException("不支持的调用目标: " + call.callee().getClass().getSimpleName());
        };

        // 为返回值分配新寄存器，生成 Call 指令
        IRVirtualRegister dest = ctx.newRegister();
        ctx.addInstruction(new CallInstruction(dest, callee, new ArrayList<>(argv)));
        return dest;
    }

    /**
     * 二元表达式构建，结果存储到新寄存器。
     * <br>
     * 支持算术、位运算与比较（==, !=, >, <, ...）。
     *
     * @param bin 二元表达式节点
     * @return 存储表达式结果的虚拟寄存器
     */
    private IRVirtualRegister buildBinary(BinaryExpressionNode bin) {
        // 递归生成左、右子表达式的寄存器
        IRVirtualRegister a = build(bin.left());
        IRVirtualRegister b = build(bin.right());
        String op = bin.operator();

        // 比较运算符（==、!=、>、< 等），需要生成条件跳转或布尔值寄存器
        if (ComparisonUtils.isComparisonOperator(op)) {
            return InstructionFactory.binOp(
                    ctx,
                    // 通过比较工具获得合适的 IR 操作码
                    ComparisonUtils.cmpOp(ctx.getScope().getVarTypes(), op, bin.left(), bin.right()),
                    a, b);
        }

        // 其它算术/位运算
        IROpCode code = ExpressionUtils.resolveOpCode(op, bin.left(), bin.right());
        if (code == null) throw new IllegalStateException("不支持的运算符: " + op);
        return InstructionFactory.binOp(ctx, code, a, b);
    }

    /**
     * 二元表达式构建，结果直接写入目标寄存器（用于赋值左值等优化场景）。
     *
     * @param bin 二元表达式节点
     * @param dest 目标虚拟寄存器
     */
    private void buildBinaryInto(BinaryExpressionNode bin, IRVirtualRegister dest) {
        IRVirtualRegister a = build(bin.left());
        IRVirtualRegister b = build(bin.right());
        String op = bin.operator();

        if (ComparisonUtils.isComparisonOperator(op)) {
            InstructionFactory.binOpInto(
                    ctx,
                    ComparisonUtils.cmpOp(ctx.getScope().getVarTypes(), op, bin.left(), bin.right()),
                    a, b, dest);
        } else {
            IROpCode code = ExpressionUtils.resolveOpCode(op, bin.left(), bin.right());
            if (code == null) throw new IllegalStateException("不支持的运算符: " + op);
            InstructionFactory.binOpInto(ctx, code, a, b, dest);
        }
    }

    /* ───────────────── 字面量辅助方法 ───────────────── */

    /**
     * 构建数字字面量表达式（如 123），分配新寄存器并生成 LoadConst 指令。
     *
     * @param value 字面量文本（字符串格式）
     * @return 存储该字面量的寄存器
     */
    private IRVirtualRegister buildNumberLiteral(String value) {
        IRConstant c = ExpressionUtils.buildNumberConstant(ctx, value);
        IRVirtualRegister r = ctx.newRegister();
        ctx.addInstruction(new LoadConstInstruction(r, c));
        return r;
    }

    /**
     * 构建字符串字面量表达式，分配新寄存器并生成 LoadConst 指令。
     *
     * @param value 字符串内容
     * @return 存储该字符串的寄存器
     */
    private IRVirtualRegister buildStringLiteral(String value) {
        IRConstant c = new IRConstant(value);
        IRVirtualRegister r = ctx.newRegister();
        ctx.addInstruction(new LoadConstInstruction(r, c));
        return r;
    }

    /**
     * 构建布尔字面量表达式（true/false），分配新寄存器并生成 LoadConst 指令（1 表示 true，0 表示 false）。
     *
     * @param v 布尔值
     * @return 存储 1/0 的寄存器
     */
    private IRVirtualRegister buildBoolLiteral(boolean v) {
        IRConstant c = new IRConstant(v ? 1 : 0);
        IRVirtualRegister r = ctx.newRegister();
        ctx.addInstruction(new LoadConstInstruction(r, c));
        return r;
    }
}
