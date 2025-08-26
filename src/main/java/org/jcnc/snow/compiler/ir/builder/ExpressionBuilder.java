package org.jcnc.snow.compiler.ir.builder;

import org.jcnc.snow.compiler.ir.common.GlobalConstTable;
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

import java.util.ArrayList;
import java.util.List;

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
 *   <li>支持 IndexExpressionNode 的编译期折叠（arr[2]），并在运行时降级为 __index_i/__index_r</li>
 * </ul>
 */
public record ExpressionBuilder(IRContext ctx) {

    /**
     * 构建表达式，返回结果寄存器。
     */
    public IRVirtualRegister build(ExpressionNode expr) {
        return switch (expr) {
            // 数字字面量，例如 123、3.14
            case NumberLiteralNode n -> buildNumberLiteral(n.value());
            // 字符串字面量，例如 "abc"
            case StringLiteralNode s -> buildStringLiteral(s.value());
            // 布尔字面量，例如 true / false
            case BoolLiteralNode b -> buildBoolLiteral(b.getValue());
            // 标识符（变量名），如 a、b
            case IdentifierNode id -> {
                // 查找当前作用域中的变量寄存器
                IRVirtualRegister reg = ctx.getScope().lookup(id.name());
                if (reg == null) throw new IllegalStateException("未定义标识符: " + id.name());
                yield reg;
            }
            // 模块常量 / 全局变量，如 ModuleA.a
            case MemberExpressionNode mem -> buildMember(mem);
            // 二元表达式（如 a+b, x==y）
            case BinaryExpressionNode bin -> buildBinary(bin);
            // 函数/方法调用表达式
            case CallExpressionNode call -> buildCall(call);
            // 一元表达式（如 -a, !a）
            case UnaryExpressionNode un -> buildUnary(un);
            case IndexExpressionNode idx -> buildIndex(idx);
            case ArrayLiteralNode arr -> buildArrayLiteral(arr);
            // 默认分支：遇到未知表达式类型则直接抛异常
            default -> throw new IllegalStateException(
                    "不支持的表达式类型: " + expr.getClass().getSimpleName());
        };
    }

    /**
     * 成员访问表达式构建
     *
     * @param mem 成员表达式节点
     * @return 存储结果的虚拟寄存器
     */
    private IRVirtualRegister buildMember(MemberExpressionNode mem) {
        if (!(mem.object() instanceof IdentifierNode id)) {
            throw new IllegalStateException("不支持的成员访问对象类型: "
                    + mem.object().getClass().getSimpleName());
        }
        String qualified = id.name() + "." + mem.member();

        /* 1) 尝试直接获取已有寄存器绑定 */
        IRVirtualRegister reg = ctx.getScope().lookup(qualified);
        if (reg != null) {
            return reg;
        }

        /* 2) 折叠为编译期常量：先查作用域，再查全局常量表 */
        Object v = ctx.getScope().getConstValue(qualified);
        if (v == null) {
            v = GlobalConstTable.get(qualified);
        }
        if (v != null) {
            IRVirtualRegister r = ctx.newRegister();
            ctx.addInstruction(new LoadConstInstruction(r, new IRConstant(v)));
            return r;
        }

        throw new IllegalStateException("未定义的常量: " + qualified);
    }


    /* ───────────────── 写入指定寄存器 ───────────────── */

    /**
     * 将表达式节点 {@link ExpressionNode} 的结果写入指定的虚拟寄存器 {@code dest}。
     * <p>
     * 按表达式类型分派处理，包括：
     * <ul>
     *   <li>字面量（数字、字符串、布尔、数组）：生成 loadConst 指令直接写入目标寄存器</li>
     *   <li>变量标识符：查表获取源寄存器，并 move 到目标寄存器</li>
     *   <li>二元表达式、下标、调用表达式：递归生成子表达式结果，并写入目标寄存器</li>
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
            // 数字字面量：生成 loadConst 指令，将数值常量写入目标寄存器
            case NumberLiteralNode n -> InstructionFactory.loadConstInto(
                    ctx, dest, ExpressionUtils.buildNumberConstant(ctx, n.value()));

            // 字符串字面量：生成 loadConst 指令，将字符串常量写入目标寄存器
            case StringLiteralNode s -> InstructionFactory.loadConstInto(
                    ctx, dest, new IRConstant(s.value()));

            // 布尔字面量：转换为 int 1/0，生成 loadConst 指令写入目标寄存器
            case BoolLiteralNode b -> InstructionFactory.loadConstInto(
                    ctx, dest, new IRConstant(b.getValue() ? 1 : 0));

            // 数组字面量：生成数组常量并写入目标寄存器
            case ArrayLiteralNode arr -> InstructionFactory.loadConstInto(
                    ctx, dest, buildArrayConstant(arr));

            // 变量标识符：查表获得源寄存器，move 到目标寄存器
            case IdentifierNode id -> {
                IRVirtualRegister src = ctx.getScope().lookup(id.name());
                if (src == null)
                    throw new IllegalStateException("未定义标识符: " + id.name());
                InstructionFactory.move(ctx, src, dest);
            }

            // 二元表达式：递归生成左右子表达式，并将结果写入目标寄存器
            case BinaryExpressionNode bin -> buildBinaryInto(bin, dest);

            // 下标表达式：递归生成索引结果，move 到目标寄存器
            case IndexExpressionNode idx -> {
                IRVirtualRegister tmp = buildIndex(idx);
                InstructionFactory.move(ctx, tmp, dest);
            }

            // 调用表达式：递归生成调用结果，move 到目标寄存器
            case CallExpressionNode call -> {
                IRVirtualRegister tmp = buildCall(call);
                InstructionFactory.move(ctx, tmp, dest);
            }

            // 其它类型：统一先 build 到临时寄存器，再 move 到目标寄存器
            default -> {
                IRVirtualRegister tmp = build(node);
                InstructionFactory.move(ctx, tmp, dest);
            }
        }
    }


    /**
     * 构建下标访问表达式（IndexExpressionNode）。
     * <ul>
     *   <li>若数组和下标均为编译期常量，则直接进行常量折叠（直接返回目标常量寄存器）；</li>
     *   <li>否则：
     *     <ul>
     *       <li>若数组表达式本身是下一个下标的中间值（即多维数组链式下标），则先用 __index_r 获取“引用”；</li>
     *       <li>最后一级用 __index_b/s/i/l/f/d/r，按声明类型智能分派。</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * @param node 下标访问表达式节点
     * @return 存放结果的虚拟寄存器
     */
    private IRVirtualRegister buildIndex(IndexExpressionNode node) {
        // 1. 常量折叠：如果 array 和 index 都是编译期常量，直接取值
        Object arrConst = tryFoldConst(node.array());
        Object idxConst = tryFoldConst(node.index());
        if (arrConst instanceof java.util.List<?> list && idxConst instanceof Number num) {
            int i = num.intValue();
            // 越界检查
            if (i < 0 || i >= list.size())
                throw new IllegalStateException("数组下标越界: " + i + " (长度 " + list.size() + ")");
            Object elem = list.get(i);
            IRVirtualRegister r = ctx.newRegister();
            // 加载常量元素到新寄存器
            InstructionFactory.loadConstInto(ctx, r, new IRConstant(elem));
            return r;
        }

        // 2. 处理多级下标（如 arr[1][2]）：中间层用 __index_r 返回“引用”
        IRVirtualRegister arrReg = (node.array() instanceof IndexExpressionNode inner)
                ? buildIndexRef(inner)   // 递归获取引用
                : build(node.array());   // 否则直接生成 array 的值

        // 3. 生成下标值
        IRVirtualRegister idxReg = build(node.index());

        // 4. 创建目标寄存器
        IRVirtualRegister dest = ctx.newRegister();

        // 5. 准备参数
        List<IRValue> argv = new ArrayList<>();
        argv.add(arrReg);
        argv.add(idxReg);

        // 6. 选择调用指令
        if (node.array() instanceof IndexExpressionNode) {
            // 非最末层，下标取“引用”
            ctx.addInstruction(new CallInstruction(dest, "__index_r", argv));
        } else {
            // 最末层，下标取实际元素值，按声明类型分派
            String func = "__index_i"; // 默认整型
            if (node.array() instanceof IdentifierNode id) {
                String declType = ctx.getScope().lookupType(id.name()); // 如 "double[]"、"int[]"
                if (declType != null) {
                    String base = declType.toLowerCase();
                    int p = base.indexOf('[');
                    if (p > 0) base = base.substring(0, p); // 基本类型
                    switch (base) {
                        case "byte" -> func = "__index_b";
                        case "short" -> func = "__index_s";
                        case "int" -> func = "__index_i";
                        case "long" -> func = "__index_l";
                        case "float" -> func = "__index_f";
                        case "double" -> func = "__index_d";
                        case "boolean" -> func = "__index_i"; // 布尔型用 int 通道返回 1/0
                        case "string" -> func = "__index_r"; // 字符串/其它未识别类型均走引用
                        default -> func = "__index_r";
                    }
                }
            }
            ctx.addInstruction(new CallInstruction(dest, func, argv));
        }

        return dest;
    }

    /**
     * 构建中间层下标访问表达式（返回引用）。
     * <p>
     * 用于多维数组的链式下标访问（如 arr[1][2]），保证中间结果是“可被再次下标”的引用。
     * <ul>
     *   <li>若数组和下标均为编译期常量，则直接常量折叠，返回目标常量寄存器；</li>
     *   <li>否则，递归处理 array，生成“引用”指令（__index_r）。</li>
     * </ul>
     * </p>
     *
     * @param node 下标访问表达式节点
     * @return 存放引用结果的虚拟寄存器
     */
    public IRVirtualRegister buildIndexRef(IndexExpressionNode node) {
        // 1. 常量折叠：如果 array 和 index 都是编译期常量，直接取值
        Object arrConst = tryFoldConst(node.array());
        Object idxConst = tryFoldConst(node.index());
        if (arrConst instanceof java.util.List<?> list && idxConst instanceof Number num) {
            int i = num.intValue();
            // 越界检查
            if (i < 0 || i >= list.size())
                throw new IllegalStateException("数组下标越界: " + i + " (长度 " + list.size() + ")");
            Object elem = list.get(i);
            IRVirtualRegister r = ctx.newRegister();
            // 加载常量元素到新寄存器
            InstructionFactory.loadConstInto(ctx, r, new IRConstant(elem));
            return r;
        }

        // 2. 递归生成 array 的“引用”，用于支持链式多级下标
        IRVirtualRegister arrReg = (node.array() instanceof IndexExpressionNode inner)
                ? buildIndexRef(inner)    // 递归向下返回引用
                : build(node.array());    // 基础数组直接 build

        // 3. 生成下标值
        IRVirtualRegister idxReg = build(node.index());

        // 4. 创建目标寄存器
        IRVirtualRegister dest = ctx.newRegister();

        // 5. 组织参数列表
        List<IRValue> argv = new ArrayList<>();
        argv.add(arrReg);
        argv.add(idxReg);

        // 6. 生成 __index_r 调用指令（总是返回引用）
        ctx.addInstruction(new CallInstruction(dest, "__index_r", argv));

        return dest;
    }

    /**
     * 常量折叠工具（支持嵌套数组）。
     * <p>
     * 尝试将表达式节点 {@code expr} 折叠为常量值，用于编译期计算/优化。
     * <ul>
     *   <li>数字字面量：返回 int 或 double。</li>
     *   <li>字符串字面量：返回字符串。</li>
     *   <li>布尔字面量：返回 1（true）或 0（false）。</li>
     *   <li>数组字面量：递归折叠所有元素为 List，如果有一项不能折叠则返回 null。</li>
     *   <li>标识符：尝试从作用域查找编译期常量值。</li>
     *   <li>其它类型：无法折叠，返回 null。</li>
     * </ul>
     * </p>
     *
     * @param expr 需要折叠的表达式节点
     * @return 编译期常量值（支持 int、double、String、List），否则返回 null
     */
    private Object tryFoldConst(ExpressionNode expr) {
        if (expr == null) return null;

        // 数字字面量：尝试解析为 int 或 double
        if (expr instanceof NumberLiteralNode n) {
            String s = n.value();
            try {
                if (s.contains(".") || s.contains("e") || s.contains("E"))
                    return Double.parseDouble(s); // 带小数或科学计数法为 double
                return Integer.parseInt(s);      // 否则为 int
            } catch (NumberFormatException e) {
                return null; // 无法解析为数字
            }
        }

        // 字符串字面量：直接返回字符串
        if (expr instanceof StringLiteralNode s) return s.value();

        // 布尔字面量：true 返回 1，false 返回 0
        if (expr instanceof BoolLiteralNode b) return b.getValue() ? 1 : 0;

        // 数组字面量：递归折叠所有元素
        if (expr instanceof ArrayLiteralNode arr) {
            List<Object> list = new ArrayList<>();
            for (ExpressionNode e : arr.elements()) {
                Object v = tryFoldConst(e);
                if (v == null) return null; // 有一项无法折叠则整体失败
                list.add(v);
            }
            return List.copyOf(list);
        }

        // 标识符：尝试查找作用域中的常量值
        if (expr instanceof IdentifierNode id) {
            Object v = null;
            try {
                v = ctx.getScope().getConstValue(id.name());
            } catch (Throwable ignored) {
                // 查不到常量或异常都视为无法折叠
            }
            return v;
        }

        // 其它类型：不支持折叠，返回 null
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
        IRVirtualRegister src = build(un.operand());
        // 分配目标寄存器
        IRVirtualRegister dest = ctx.newRegister();
        switch (un.operator()) {
            // 算术负号：生成取负指令（例如：-a）
            case "-" -> ctx.addInstruction(
                    new UnaryOperationInstruction(ExpressionUtils.negOp(un.operand()), dest, src));

            // 逻辑非：等价于 a == 0，生成整数等于比较指令（!a）
            case "!" -> {
                // 生成常量0的寄存器
                IRVirtualRegister zero = InstructionFactory.loadConst(ctx, 0);
                // 比较 src 是否等于0，等价于逻辑非
                return InstructionFactory.binOp(ctx, IROpCode.CMP_IEQ, src, zero);
            }
            // 其它一元运算符不支持，抛出异常
            default -> throw new IllegalStateException("未知一元运算符: " + un.operator());
        }
        return dest;
    }


    /**
     * 构建函数或方法调用表达式。
     * <p>
     * 支持普通函数调用（foo(a, b)）与成员方法调用（obj.method(a, b)）。
     * <ul>
     *   <li>首先递归生成所有参数的虚拟寄存器列表。</li>
     *   <li>根据 callee 类型区分成员访问或直接标识符调用，并规范化方法名（如加前缀）。</li>
     *   <li>为返回值分配新寄存器，生成 Call 指令。</li>
     * </ul>
     * </p>
     *
     * @param call 函数/方法调用表达式节点
     * @return 存放调用结果的虚拟寄存器
     */
    private IRVirtualRegister buildCall(CallExpressionNode call) {
        // 1. 递归生成所有参数的寄存器
        List<IRVirtualRegister> argv = call.arguments().stream().map(this::build).toList();

        // 2. 规范化被调用方法名（区分成员方法与普通函数）
        String callee = switch (call.callee()) {
            // 成员方法调用，如 obj.method()
            case MemberExpressionNode m when m.object() instanceof IdentifierNode id -> id.name() + "." + m.member();
            // 普通函数调用，或处理命名空间前缀（如当前方法名为 namespace.func）
            case IdentifierNode id -> {
                String current = ctx.getFunction().name();
                int dot = current.lastIndexOf('.');
                if (dot > 0)
                    yield current.substring(0, dot) + "." + id.name(); // 同命名空间内调用
                yield id.name(); // 全局函数调用
            }
            // 其它类型不支持
            default -> throw new IllegalStateException(
                    "不支持的调用目标: " + call.callee().getClass().getSimpleName());
        };

        // 3. 分配用于存放返回值的新寄存器，并生成 Call 指令
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

        // 判断是否为比较运算符（==、!=、>、<等）
        if (ComparisonUtils.isComparisonOperator(op)) {
            // 生成比较操作，返回布尔值寄存器
            return InstructionFactory.binOp(
                    ctx,
                    // 根据运算符和操作数类型获得合适的 IR 操作码
                    ComparisonUtils.cmpOp(ctx.getScope().getVarTypes(), op, bin.left(), bin.right()),
                    a, b);
        }

        // 其它二元运算（算术、位运算等）
        IROpCode code = ExpressionUtils.resolveOpCode(op, bin.left(), bin.right());
        if (code == null)
            throw new IllegalStateException("不支持的运算符: " + op);
        // 生成二元操作指令
        return InstructionFactory.binOp(ctx, code, a, b);
    }

    /**
     * 二元表达式构建，结果直接写入目标寄存器（用于赋值左值等优化场景）。
     *
     * @param bin  二元表达式节点
     * @param dest 目标虚拟寄存器
     */
    private void buildBinaryInto(BinaryExpressionNode bin, IRVirtualRegister dest) {
        // 递归生成左、右操作数的寄存器
        IRVirtualRegister a = build(bin.left());
        IRVirtualRegister b = build(bin.right());
        String op = bin.operator();

        // 处理比较运算符（==、!=、>、< 等）
        if (ComparisonUtils.isComparisonOperator(op)) {
            InstructionFactory.binOpInto(
                    ctx,
                    // 选择对应类型和符号的比较操作码
                    ComparisonUtils.cmpOp(ctx.getScope().getVarTypes(), op, bin.left(), bin.right()),
                    a, b, dest);
        } else {
            // 算术或位运算符
            IROpCode code = ExpressionUtils.resolveOpCode(op, bin.left(), bin.right());
            if (code == null)
                throw new IllegalStateException("不支持的运算符: " + op);
            // 生成二元操作指令，写入目标寄存器
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
        // 解析数字常量
        IRConstant c = ExpressionUtils.buildNumberConstant(ctx, value);
        // 分配新寄存器
        IRVirtualRegister r = ctx.newRegister();
        // 生成 LoadConst 指令
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
        // 构建字符串常量
        IRConstant c = new IRConstant(value);
        // 分配新寄存器
        IRVirtualRegister r = ctx.newRegister();
        // 生成 LoadConst 指令
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
        // 转换为 1 或 0 的常量
        IRConstant c = new IRConstant(v ? 1 : 0);
        // 分配新寄存器
        IRVirtualRegister r = ctx.newRegister();
        // 生成 LoadConst 指令
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
        // 递归生成支持嵌套的数组常量
        IRConstant c = buildArrayConstant(arr);
        // 分配新寄存器
        IRVirtualRegister r = ctx.newRegister();
        // 生成 LoadConst 指令
        ctx.addInstruction(new LoadConstInstruction(r, c));
        return r;
    }

    /**
     * 构建支持嵌套的数组常量表达式。
     * <p>
     * 遍历并递归处理数组字面量的所有元素：
     * <ul>
     *   <li>数字字面量：根据内容生成 int 或 double 常量</li>
     *   <li>字符串字面量：直接存储字符串内容</li>
     *   <li>布尔字面量：转换为 1（true）或 0（false）存储</li>
     *   <li>数组字面量：递归构建，允许多层嵌套，最终生成嵌套的 List</li>
     * </ul>
     * 若包含非常量元素，则抛出异常。
     * </p>
     *
     * @param arr 数组字面量节点
     * @return 封装所有常量元素（支持嵌套 List）的 {@link IRConstant}
     * @throws IllegalStateException 如果数组中存在非常量元素
     */

    private IRConstant buildArrayConstant(ArrayLiteralNode arr) {
        List<Object> list = new ArrayList<>();
        for (ExpressionNode e : arr.elements()) {
            switch (e) {
                // 数字字面量，解析并加入
                case NumberLiteralNode n -> {
                    IRConstant num = ExpressionUtils.buildNumberConstant(ctx, n.value());
                    list.add(num.value());
                }
                // 字符串字面量，直接加入
                case StringLiteralNode s -> list.add(s.value());
                // 布尔字面量，转成 1/0
                case BoolLiteralNode b -> list.add(b.getValue() ? 1 : 0);
                // 嵌套数组，递归生成并加入
                case ArrayLiteralNode inner -> {
                    IRConstant innerConst = buildArrayConstant(inner);
                    list.add(innerConst.value());
                }
                // 其它类型暂不支持
                default -> throw new IllegalStateException(
                        "暂不支持含非常量元素的数组字面量: " + e.getClass().getSimpleName());
            }
        }
        // 返回不可变的 List 封装为 IRConstant
        return new IRConstant(List.copyOf(list));
    }

}
