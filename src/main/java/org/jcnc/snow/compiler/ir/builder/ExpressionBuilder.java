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
            case IdentifierNode id -> buildIdentifier(id);
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
            case NewExpressionNode n -> buildNew(n);
            // 默认分支：遇到未知表达式类型则直接抛异常
            default -> throw new IllegalStateException(
                    "不支持的表达式类型: " + expr.getClass().getSimpleName());
        };
    }

    /**
     * 构造标识符节点对应的 IR 虚拟寄存器。
     * <p>
     * 支持普通变量查找，以及结构体方法/构造器中的“字段回退”机制（即自动将裸标识符回退为 this.<id>）。
     *
     * @param id 标识符节点
     * @return 查找到的 IR 虚拟寄存器
     * @throws IllegalStateException 若标识符未定义且无法回退为字段
     */
    private IRVirtualRegister buildIdentifier(IdentifierNode id) {
        // ====================== 普通变量查找 ======================
        // 1. 在当前作用域查找变量（可能是局部变量、形参、临时变量等）
        IRVirtualRegister reg = ctx.getScope().lookup(id.name());
        if (reg != null) return reg;

        // ====================== 字段回退机制 ======================
        // 2. 若未找到，则判断是否处于结构体方法或构造器中
        //    尝试将裸标识符自动视为 this.<id>，即访问当前结构体实例的成员字段
        IRVirtualRegister thisReg = ctx.getScope().lookup("this");
        String thisType = ctx.getScope().lookupType("this");
        if (thisReg != null && thisType != null) {
            // 生成成员表达式节点 this.<id>
            MemberExpressionNode asField = new MemberExpressionNode(
                    new IdentifierNode("this", id.context()), // 构造 this 节点
                    id.name(),                                // 字段名
                    id.context()
            );
            // 递归构造成员访问（相当于构造 this.<id> 的 IR）
            return buildMember(asField);
        }

        // ====================== 标识符未定义异常 ======================
        // 3. 无法查找到变量且不能字段回退，抛出异常
        throw new IllegalStateException("未定义标识符: " + id.name());
    }


    /**
     * 构建成员访问表达式的 IR 虚拟寄存器。
     * <p>
     * 支持两类成员访问：
     * <ul>
     *     <li>模块常量访问（如 ModuleName.CONST）</li>
     *     <li>结构体/对象字段访问（如 obj.field 或 this.field）</li>
     * </ul>
     *
     * @param mem 成员访问表达式节点（如 a.b 或 ModuleName.CONST）
     * @return 存储成员值的 IR 虚拟寄存器
     * @throws IllegalStateException 若找不到成员或无法解析类型
     */
    private IRVirtualRegister buildMember(MemberExpressionNode mem) {
        // ===== 1. 处理模块常量 (ModuleName.member) =====
        // 检查成员访问的对象是否为一个标识符（即模块名）
        if (mem.object() instanceof IdentifierNode oid) {
            String mod = oid.name();
            // 查找是否存在该模块下的全局常量定义
            Object c = GlobalConstTable.get(mod + "." + mem.member());
            if (c != null) {
                // 若找到常量，分配新寄存器并生成 LoadConst 指令
                IRVirtualRegister r = ctx.newRegister();
                ctx.addInstruction(new LoadConstInstruction(r, new IRConstant(c)));
                return r;
            }
        }

        // ===== 2. 结构体/对象字段访问 =====

        // 1 递归构建成员对象（如 a.b，先获得 a 的寄存器）
        IRVirtualRegister objReg = build(mem.object());

        // 2 尝试解析成员访问接收者（object）的类型
        String ownerType = null;
        if (mem.object() instanceof IdentifierNode oid) {
            // 如果对象是标识符，直接查询其类型（例如 a.x，a 的类型）
            ownerType = ctx.getScope().lookupType(oid.name());
        }
        if (ownerType == null || ownerType.isEmpty()) {
            // 兜底：如果访问 this.xxx，并且 this 有类型，则使用 this 的类型
            String thisType = ctx.getScope().lookupType("this");
            if (thisType != null) ownerType = thisType;
        }
        if (ownerType == null || ownerType.isEmpty()) {
            // 类型无法解析，抛出异常
            throw new IllegalStateException("无法解析成员访问接收者的类型");
        }

        // 3 查找字段槽位下标：ownerType 的 mem.member() 字段的序号
        Integer fieldIndex = ctx.getScope().lookupFieldIndex(ownerType, mem.member());
        if (fieldIndex == null) {
            // 字段不存在，抛出异常
            throw new IllegalStateException("类型 " + ownerType + " 不存在字段: " + mem.member());
        }

        // 4 生成读取字段的 IR 指令：CALL __index_r, objReg, const(fieldIndex)
        // 4.1 先将字段下标加载到寄存器
        IRVirtualRegister idxReg = ctx.newRegister();
        ctx.addInstruction(new LoadConstInstruction(
                idxReg,
                IRConstant.fromNumber(Integer.toString(fieldIndex))
        ));

        // 4.2 生成成员读取调用指令
        IRVirtualRegister out = ctx.newRegister();
        List<IRValue> args = new ArrayList<>();
        args.add(objReg); // 对象寄存器
        args.add(idxReg); // 字段下标寄存器
        ctx.addInstruction(new CallInstruction(out, "__index_r", args));
        return out;
    }


    /**
     * 将表达式节点 {@link ExpressionNode} 的求值结果写入指定的目标虚拟寄存器 {@code dest}。
     * <p>
     * 根据不同表达式类型，采取高效或递归方式生成中间代码，涵盖常量、变量、运算、数组、调用等。
     * </p>
     *
     * @param node 表达式节点（可为字面量、变量、数组、运算等）
     * @param dest 目标虚拟寄存器（写入结果）
     * @throws IllegalStateException 若变量标识符未定义
     */
    public void buildInto(ExpressionNode node, IRVirtualRegister dest) {
        switch (node) {
            // ===================== 数字字面量 =====================
            // 如 42、3.14 等，直接生成 loadConst 指令，常量写入目标寄存器
            case NumberLiteralNode n -> InstructionFactory.loadConstInto(
                    ctx, dest, ExpressionUtils.buildNumberConstant(ctx, n.value()));

            // ===================== 字符串字面量 =====================
            // 如 "hello"，直接生成 loadConst 指令
            case StringLiteralNode s -> InstructionFactory.loadConstInto(
                    ctx, dest, new IRConstant(s.value()));

            // ===================== 布尔字面量 =====================
            // 如 true/false，转换为 int 常量 1/0，生成 loadConst
            case BoolLiteralNode b -> InstructionFactory.loadConstInto(
                    ctx, dest, new IRConstant(b.getValue() ? 1 : 0));

            // ===================== 数组字面量 =====================
            // 直接构造数组常量（只支持静态初始化），生成 loadConst
            case ArrayLiteralNode arr -> InstructionFactory.loadConstInto(
                    ctx, dest, buildArrayConstant(arr));

            // ===================== new 表达式（构造数组/对象） =====================
            // 生成空数组，然后按参数依次初始化每一项，使用 __setindex_r 填充目标寄存器
            case NewExpressionNode newExpr -> {
                // 1. 把空列表写入目标寄存器
                InstructionFactory.loadConstInto(ctx, dest, new IRConstant(java.util.List.of()));

                // 2. 依次写入构造实参，同时缓存参数寄存器
                List<IRVirtualRegister> argRegs = new ArrayList<>();
                for (int i = 0; i < newExpr.arguments().size(); i++) {
                    IRVirtualRegister argReg = build(newExpr.arguments().get(i));
                    argRegs.add(argReg);

                    IRVirtualRegister idxReg = ctx.newTempRegister();
                    InstructionFactory.loadConstInto(ctx, idxReg, new IRConstant(i));

                    ctx.addInstruction(new CallInstruction(
                            null,
                            "__setindex_r",
                            List.of(dest, idxReg, argReg)));
                }

                /* 3. 若确认为结构体，显式调用 <Struct>.__init__N 完成字段初始化 */
                if (IRBuilderScope.getStructLayout(newExpr.typeName()) != null) {
                    String ctor = newExpr.typeName() + ".__init__" + argRegs.size();
                    List<IRValue> ctorArgs = new ArrayList<>();
                    ctorArgs.add(dest);          // this
                    ctorArgs.addAll(argRegs);    // 实参
                    ctx.addInstruction(new CallInstruction(null, ctor, ctorArgs));
                }
            }


            // ===================== 变量标识符 =====================
            // 如 x，查找符号表，move 到目标寄存器。未定义时报错。
            case IdentifierNode id -> {
                IRVirtualRegister src = ctx.getScope().lookup(id.name());
                if (src == null)
                    throw new IllegalStateException("未定义标识符: " + id.name());
                InstructionFactory.move(ctx, src, dest);
            }

            // ===================== 二元表达式（如 a + b） =====================
            // 递归生成左右操作数，并将运算结果写入目标寄存器
            case BinaryExpressionNode bin -> buildBinaryInto(bin, dest);

            // ===================== 下标访问（如 arr[1]） =====================
            // 先递归构造表达式，将索引结果 move 到目标寄存器
            case IndexExpressionNode idx -> {
                IRVirtualRegister tmp = buildIndex(idx);
                InstructionFactory.move(ctx, tmp, dest);
            }

            // ===================== 函数/方法调用（如 foo(a, b)） =====================
            // 递归生成调用，结果 move 到目标寄存器
            case CallExpressionNode call -> {
                IRVirtualRegister tmp = buildCall(call);
                InstructionFactory.move(ctx, tmp, dest);
            }

            // ===================== 其它所有情况（兜底处理） =====================
            // 通用流程：先生成结果到临时寄存器，再 move 到目标寄存器
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
        switch (expr) {
            case null -> {
                return null;
            }

            // 数字字面量：尝试解析为 int 或 double
            case NumberLiteralNode n -> {
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
            case StringLiteralNode s -> {
                return s.value();
            }

            // 布尔字面量：true 返回 1，false 返回 0
            case BoolLiteralNode b -> {
                return b.getValue() ? 1 : 0;
            }

            // 数组字面量：递归折叠所有元素
            case ArrayLiteralNode arr -> {
                List<Object> list = new ArrayList<>();
                for (ExpressionNode e : arr.elements()) {
                    Object v = tryFoldConst(e);
                    if (v == null) return null; // 有一项无法折叠则整体失败
                    list.add(v);
                }
                return List.copyOf(list);
            }


            // 标识符：尝试查找作用域中的常量值
            case IdentifierNode id -> {
                Object v = null;
                try {
                    v = ctx.getScope().getConstValue(id.name());
                } catch (Throwable ignored) {
                    // 查不到常量或异常都视为无法折叠
                }
                return v;
            }
            default -> {
            }
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
     * 构造 new 对象/数组创建表达式的 IR 虚拟寄存器（支持列表型构造）。
     * <p>
     * 语义说明：本方法用于生成“new 表达式”的中间代码流程，具体包括：
     * <ul>
     *     <li>分配一个新的寄存器用于保存新对象/数组引用</li>
     *     <li>将一个空列表常量写入该寄存器（后端 runtime 识别为可变列表/对象）</li>
     *     <li>遍历所有构造参数，依次写入目标列表的 [0..n-1] 位置</li>
     *     <li>最终返回该寄存器</li>
     * </ul>
     *
     * @param node new 表达式节点，包含所有构造参数
     * @return 保存新创建对象/数组引用的目标寄存器
     */
    private IRVirtualRegister buildNew(NewExpressionNode node) {
        // 1. 分配新的寄存器作为 new 表达式的结果
        IRVirtualRegister dest = ctx.newRegister();

        /* 1. 写入空列表 */
        InstructionFactory.loadConstInto(ctx, dest, new IRConstant(java.util.List.of()));

        /* 2. 创建参数并写入列表，同时缓存参数寄存器 */
        List<IRVirtualRegister> argRegs = new ArrayList<>();
        for (int i = 0; i < node.arguments().size(); i++) {
            IRVirtualRegister argReg = build(node.arguments().get(i));
            argRegs.add(argReg);

            IRVirtualRegister idxReg = ctx.newTempRegister();
            InstructionFactory.loadConstInto(ctx, idxReg, new IRConstant(i));

            ctx.addInstruction(new CallInstruction(
                    null, "__setindex_r",
                    List.of(dest, idxReg, argReg)));
        }

        /* 3. 若为结构体实例，调用构造器 <Struct>.__init__N */
        if (IRBuilderScope.getStructLayout(node.typeName()) != null) {
            String ctorName = node.typeName() + ".__init__" + argRegs.size();
            List<IRValue> ctorArgs = new ArrayList<>();
            ctorArgs.add(dest);          // 隐式 this
            ctorArgs.addAll(argRegs);    // 构造实参
            ctx.addInstruction(new CallInstruction(null, ctorName, ctorArgs));
        }

        return dest;
    }


    /**
     * 构建函数或方法调用表达式的 IR 指令，并返回结果寄存器。
     * <p>
     * 支持五类调用：
     * <ul>
     *   <li>普通函数调用（foo(a, b)）</li>
     *   <li>成员/模块静态方法调用（obj.method(...) 或 Module.func(...)）</li>
     *   <li>链式方法调用（obj.get().foo()）</li>
     *   <li>super(...) 调父类构造函数</li>
     *   <li><b>super.method(...)</b> 调父类实例方法 <b>(新增)</b></li>
     * </ul>
     * 核心流程：
     * <ol>
     *     <li>递归生成所有参数的虚拟寄存器列表</li>
     *     <li>根据 callee 类型区分结构体方法/模块静态函数/普通函数</li>
     *     <li>规范化被调用方法名，并整理最终参数表</li>
     *     <li>分配用于结果的新寄存器，生成 Call 指令</li>
     * </ol>
     *
     * @param call 函数或方法调用表达式节点
     * @return 存放调用结果的目标虚拟寄存器
     * @throws IllegalStateException 被调用表达式类型不支持或参数异常
     */
    private IRVirtualRegister buildCall(CallExpressionNode call) {
        // 1. 递归生成显式实参
        List<IRVirtualRegister> explicitRegs = new ArrayList<>();
        for (ExpressionNode arg : call.arguments()) explicitRegs.add(build(arg));

        String callee;                               // 规范化后的被调函数名
        List<IRValue> finalArgs = new ArrayList<>(); // 最终 CALL 参数

        // 2. 根据 callee 类型分支
        switch (call.callee()) {
            case IdentifierNode id when "super".equals(id.name()) -> {
                // ========= 情况 0: super(...) 调用父类构造函数 =========
                String thisType = ctx.getScope().lookupType("this");
                if (thisType == null)
                    throw new IllegalStateException("super(...) 只能在构造函数中使用");

                // 查找父类名
                String parent = IRBuilderScope.getStructParent(thisType);
                if (parent == null)
                    throw new IllegalStateException(thisType + " 没有父类，无法调用 super(...)");

                // 拼接父类构造函数名，例如 Person.__init__1
                callee = parent + ".__init__" + explicitRegs.size();

                // 参数表：第一个是 this，再加上传入的实参
                IRVirtualRegister thisReg = ctx.getScope().lookup("this");
                if (thisReg == null)
                    throw new IllegalStateException("未绑定 this，不能调用 super(...)");

                finalArgs.add(thisReg);
                finalArgs.addAll(explicitRegs);
            }
            case MemberExpressionNode m when m.object() instanceof IdentifierNode idObj -> {
                // ========= 情况 1: obj.method(...) 或 ModuleName.func(...) =========
                String recvName = idObj.name();

                // super.method(...)
                if ("super".equals(recvName)) {
                    // super.method(...) 调父类实例方法
                    String thisType = ctx.getScope().lookupType("this");
                    if (thisType == null)
                        throw new IllegalStateException("super.method(...) 只能在实例方法中使用");

                    String parent = IRBuilderScope.getStructParent(thisType);
                    if (parent == null)
                        throw new IllegalStateException(thisType + " 没有父类，无法调用 super.method(...)");

                    callee = parent + "." + m.member();

                    IRVirtualRegister thisReg = ctx.getScope().lookup("this");
                    if (thisReg == null)
                        throw new IllegalStateException("未绑定 this");

                    finalArgs.add(thisReg);          // 隐式 this
                    finalArgs.addAll(explicitRegs);  // 显式参数
                } else {
                    // 普通 obj.method(...) 或 Module.func(...)
                    String recvType = ctx.getScope().lookupType(recvName);

                    if (recvType == null || recvType.isEmpty()) {
                        // 模块函数调用 —— "模块名.函数名"
                        callee = recvName + "." + m.member();
                        finalArgs.addAll(explicitRegs);
                    } else {
                        // 结构体实例方法调用 —— "类型名.方法名"，且第一个参数为 this
                        callee = recvType + "." + m.member();
                        IRVirtualRegister thisReg = ctx.getScope().lookup(recvName);
                        if (thisReg == null)
                            throw new IllegalStateException("Undefined identifier: " + recvName);
                        finalArgs.add(thisReg);         // 隐式 this
                        finalArgs.addAll(explicitRegs); // 其它参数
                    }
                }
            }

            /* ===== 递归链式调用 ===== */
            case MemberExpressionNode m -> {
                // ========= 情况 2: 通用成员调用 (支持链式调用) =========
                IRVirtualRegister objReg = build(m.object()); // 递归计算 object 表达式

                callee = m.member();                          // 直接用成员名
                finalArgs.add(objReg);                        // 隐式 this
                finalArgs.addAll(explicitRegs);               // 其它参数
            }

            /* ===== 普通函数 foo(...) ===== */
            case IdentifierNode id -> {
                // ========= 情况 3: 普通函数调用 foo(...) =========
                String current = ctx.getFunction().name();  // 当前函数全名

                int dot = current.lastIndexOf('.');
                if (dot >= 0 && !id.name().contains(".")) {
                    // 自动补充模块名前缀
                    callee = current.substring(0, dot) + "." + id.name();
                } else {
                    callee = id.name();
                }
                finalArgs.addAll(explicitRegs);
            }

            /* ===== 其它不支持 ===== */
            default -> throw new IllegalStateException(
                    "Unsupported callee type: " + call.callee().getClass().getSimpleName());
        }

        // 3. 分配目标寄存器，生成函数/方法调用指令
        IRVirtualRegister dest = ctx.newRegister();
        ctx.addInstruction(new CallInstruction(dest, callee, finalArgs));
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
