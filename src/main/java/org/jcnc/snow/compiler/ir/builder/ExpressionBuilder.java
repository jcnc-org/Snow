package org.jcnc.snow.compiler.ir.builder;

import org.jcnc.snow.compiler.ir.core.IROpCode;
import org.jcnc.snow.compiler.ir.core.IRValue;
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
 * {@code ExpressionBuilder} 表达式 → IR 构建器。
 * <p>
 * 负责将 AST 表达式节点递归转换为 IR 虚拟寄存器操作，并生成对应的 IR 指令序列。
 * 支持字面量、标识符、二元表达式、一元表达式、函数调用、数组下标等多种类型表达式。
 * <ul>
 *   <li>将表达式节点映射为虚拟寄存器</li>
 *   <li>为每种表达式类型生成对应 IR 指令</li>
 *   <li>支持表达式嵌套的递归构建</li>
 *   <li>支持写入指定目标寄存器，避免冗余的 move 指令</li>
 *   <li>支持 IndexExpressionNode 的编译期折叠（arr[2]），并自动降级为运行时调用 __index_i</li>
 * </ul>
 */
public record ExpressionBuilder(IRContext ctx) {

    /**
     * 构建表达式，返回存储其结果的虚拟寄存器。
     *
     * @param expr 要生成 IR 的表达式节点
     * @return 存储表达式值的虚拟寄存器
     * @throws IllegalStateException 不支持的表达式类型或未定义标识符
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
     * 将表达式节点 {@link ExpressionNode} 的结果写入指定的虚拟寄存器 {@code dest}。
     * <p>
     * 按表达式类型分派处理，包括：
     * <ul>
     *   <li>字面量（数字、字符串、布尔、数组）：生成 loadConst 指令直接写入目标寄存器</li>
     *   <li>变量标识符：查表获取源寄存器，并 move 到目标寄存器</li>
     *   <li>二元表达式与下标表达式：递归生成子表达式结果，并写入目标寄存器</li>
     *   <li>其它类型：统一先 build 到临时寄存器，再 move 到目标寄存器</li>
     * </ul>
     * </p>
     *
     * @param node 要求值的表达式节点
     * @param dest 结果目标虚拟寄存器
     * @throws IllegalStateException 若标识符未定义（如变量未声明时引用）
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
            case ArrayLiteralNode arr ->
                    InstructionFactory.loadConstInto(ctx, dest, buildArrayConstant(arr));
            case IdentifierNode id -> {
                IRVirtualRegister src = ctx.getScope().lookup(id.name());
                if (src == null)
                    throw new IllegalStateException("未定义标识符: " + id.name());
                InstructionFactory.move(ctx, src, dest);
            }

            // 二元表达式：递归生成并写入目标寄存器
            case BinaryExpressionNode bin -> buildBinaryInto(bin, dest);
            case IndexExpressionNode idx -> {
                IRVirtualRegister tmp = buildIndex(idx);
                InstructionFactory.move(ctx, tmp, dest);
            }
            default -> {
                IRVirtualRegister tmp = build(node);
                InstructionFactory.move(ctx, tmp, dest);
            }
        }
    }

    /**
     * 下标访问表达式处理。支持编译期常量折叠（数组和下标均为常量时直接求值），
     * 否则生成运行时调用 __index_i（由 VM 降级为 ARR_GET）。
     *
     * @param node 下标访问表达式
     * @return 存储结果的虚拟寄存器
     */
    private IRVirtualRegister buildIndex(IndexExpressionNode node) {
        Object arrConst = tryFoldConst(node.array());
        Object idxConst = tryFoldConst(node.index());
        if (arrConst instanceof java.util.List<?> list && idxConst instanceof Number num) {
            int i = num.intValue();
            if (i < 0 || i >= list.size())
                throw new IllegalStateException("数组下标越界: " + i + " (长度 " + list.size() + ")");
            Object elem = list.get(i);
            IRVirtualRegister r = ctx.newRegister();
            InstructionFactory.loadConstInto(ctx, r, new IRConstant(elem));
            return r;
        }
        IRVirtualRegister arrReg = build(node.array());
        IRVirtualRegister idxReg = build(node.index());
        IRVirtualRegister dest   = ctx.newRegister();
        List<IRValue> argv = new ArrayList<>();
        argv.add(arrReg);
        argv.add(idxReg);
        ctx.addInstruction(new CallInstruction(dest, "__index_i", argv));
        return dest;
    }

    /**
     * 尝试将表达式折叠为编译期常量（支持嵌套）。
     * 支持数字、字符串、布尔、数组、常量标识符。
     *
     * @param expr 要折叠的表达式节点
     * @return 常量对象（如数字、字符串、List），否则返回 null
     */
    private Object tryFoldConst(ExpressionNode expr) {
        if (expr == null) return null;
        if (expr instanceof NumberLiteralNode n) {
            String s = n.value();
            try {
                if (s.contains(".") || s.contains("e") || s.contains("E")) {
                    return Double.parseDouble(s);
                }
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (expr instanceof StringLiteralNode s) {
            return s.value();
        }
        if (expr instanceof BoolLiteralNode b) {
            return b.getValue() ? 1 : 0;
        }
        if (expr instanceof ArrayLiteralNode arr) {
            java.util.List<Object> list = new java.util.ArrayList<>();
            for (ExpressionNode e : arr.elements()) {
                Object v = tryFoldConst(e);
                if (v == null) return null;
                list.add(v);
            }
            return java.util.List.copyOf(list);
        }
        if (expr instanceof IdentifierNode id) {
            Object v = null;
            try {
                v = ctx.getScope().getConstValue(id.name());
            } catch (Throwable ignored) {}
            return v;
        }
        return null;
    }

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
     * 构建函数或方法调用表达式。模块内未限定调用会自动补全当前模块名。
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
            // 普通函数调用，如果未指定模块，自动补全当前模块名
            case IdentifierNode id -> {
                String current = ctx.getFunction().name();
                int dot = current.lastIndexOf('.');
                if (dot > 0) {
                    // 当前处于模块内函数（Module.func），补全为同模块下的全限定名
                    yield current.substring(0, dot) + "." + id.name();
                } else {
                    // 顶层/脚本函数等不含模块前缀，保持原样
                    yield id.name();
                }
            }
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

    /**
     * 构建数组字面量表达式（元素均为常量时）。
     *
     * @param arr 数组字面量节点
     * @return 存储该数组的寄存器
     */
    private IRVirtualRegister buildArrayLiteral(ArrayLiteralNode arr) {
        IRConstant c = buildArrayConstant(arr);
        IRVirtualRegister r = ctx.newRegister();
        ctx.addInstruction(new LoadConstInstruction(r, c));
        return r;
    }

    /**
     * 构建数组常量（所有元素均为数字、字符串或布尔常量）。
     *
     * @param arr 数组字面量节点
     * @return 数组 IRConstant
     * @throws IllegalStateException 若含有非常量元素
     */
    private IRConstant buildArrayConstant(ArrayLiteralNode arr) {
        List<Object> list = new ArrayList<>();
        for (ExpressionNode e : arr.elements()) {
            switch (e) {
                case NumberLiteralNode n -> {
                    IRConstant num = ExpressionUtils.buildNumberConstant(ctx, n.value());
                    list.add(num.value());
                }
                case StringLiteralNode s -> list.add(s.value());
                case BoolLiteralNode b   -> list.add(b.getValue() ? 1 : 0);
                default -> throw new IllegalStateException(
                        "暂不支持含非常量元素的数组字面量: " + e.getClass().getSimpleName());
            }
        }
        return new IRConstant(List.copyOf(list));
    }
}
