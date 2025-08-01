package org.jcnc.snow.compiler.ir.builder;

import org.jcnc.snow.compiler.ir.core.IROpCode;
import org.jcnc.snow.compiler.ir.utils.ComparisonUtils;
import org.jcnc.snow.compiler.ir.utils.IROpCodeUtils;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.compiler.parser.ast.*;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;

import java.util.ArrayDeque;

/**
 * <b>StatementBuilder</b> —— AST 语句节点 ({@link StatementNode}) 到 IR 指令序列的构建器。
 * <p>
 * 负责将各类语句（如循环、分支、表达式、赋值、声明、返回、break、continue 等）
 * 转换为对应的 IR 指令，并自动管理作用域、虚拟寄存器分配以及控制流标签（如 break/continue 目标）。
 * </p>
 *
 * <ul>
 *     <li>支持多种语句类型的分发与转换。</li>
 *     <li>与 {@link ExpressionBuilder} 协作完成表达式相关 IR 生成。</li>
 *     <li>负责控制流跳转（分支、循环）的标签分配与维护。</li>
 *     <li>在变量赋值和声明时自动常量折叠和登记。</li>
 * </ul>
 *
 * @author [你的名字]
 * @since 1.0
 */
public class StatementBuilder {

    /**
     * 当前 IR 上下文，包含作用域、指令序列等信息。
     */
    private final IRContext ctx;

    /**
     * 表达式 IR 构建器，用于将表达式节点转为 IR 指令。
     */
    private final ExpressionBuilder expr;

    /**
     * break 目标标签栈（保存每层循环的结束标签，用于 break 跳转）
     */
    private final ArrayDeque<String> breakTargets = new ArrayDeque<>();

    /**
     * continue 目标标签栈（保存每层循环的 step 起始标签，用于 continue 跳转）
     */
    private final ArrayDeque<String> continueTargets = new ArrayDeque<>();

    /**
     * 构造方法。初始化 StatementBuilder。
     *
     * @param ctx IR 编译上下文环境，包含作用域、标签、指令等信息
     */
    public StatementBuilder(IRContext ctx) {
        this.ctx = ctx;
        this.expr = new ExpressionBuilder(ctx);
    }

    /**
     * 将一个 AST 语句节点转为 IR 指令序列。
     * <p>
     * 根据节点类型分发到对应的处理方法。
     * 支持：循环、分支、表达式语句、赋值、声明、返回、break、continue。
     * 不支持的语句类型会抛出异常。
     * </p>
     *
     * @param stmt 待转换的语句节点，不能为空
     * @throws IllegalStateException 若遇到不支持的语句类型，或 break/continue 不在循环体中
     */
    public void build(StatementNode stmt) {
        if (stmt instanceof LoopNode loop) {
            buildLoop(loop);
            return;
        }
        if (stmt instanceof IfNode ifNode) {
            buildIf(ifNode);
            return;
        }
        if (stmt instanceof ExpressionStatementNode(ExpressionNode exp, NodeContext _)) {
            expr.build(exp);
            return;
        }
        if (stmt instanceof AssignmentNode(String var, ExpressionNode rhs, NodeContext _)) {
            final String type = ctx.getScope().lookupType(var);
            ctx.setVarType(type);
            IRVirtualRegister target = getOrDeclareRegister(var, type);
            expr.buildInto(rhs, target);

            // 赋值时尝试记录/清除常量
            try {
                Object constVal = tryFoldConst(rhs);
                if (constVal != null)
                    ctx.getScope().setConstValue(var, constVal);
                else
                    ctx.getScope().clearConstValue(var);
            } catch (Throwable ignored) {
            }

            ctx.clearVarType();
            return;
        }
        if (stmt instanceof DeclarationNode decl) {
            // 变量声明语句（如 int a = 1;）
            if (decl.getInitializer().isPresent()) {
                // 如果声明时带有初始值（如 int a = b;）

                // 1. 设置变量类型，便于表达式求值/指令生成时推断类型信息
                ctx.setVarType(decl.getType());

                // 2. 为当前声明的变量分配一个全新的虚拟寄存器
                //    这样可以保证该变量和初始值表达式中的变量物理上独立，不会发生别名/串扰
                IRVirtualRegister dest = ctx.newRegister();

                // 3. 将初始值表达式的计算结果写入新分配的寄存器
                //    即使初始值是某个已存在变量（如 outer_i），这里是值的拷贝
                expr.buildInto(decl.getInitializer().get(), dest);

                // 声明赋初值时登记常量
                try {
                    Object constVal = tryFoldConst(decl.getInitializer().get());
                    if (constVal != null)
                        ctx.getScope().setConstValue(decl.getName(), constVal);
                    else
                        ctx.getScope().clearConstValue(decl.getName());
                } catch (Throwable ignored) {
                }

                ctx.clearVarType();
                ctx.getScope().declare(decl.getName(), decl.getType(), dest);
            } else {
                ctx.getScope().declare(decl.getName(), decl.getType());
                ctx.getScope().clearConstValue(decl.getName());
            }
            return;
        }
        if (stmt instanceof ReturnNode ret) {
            // return 语句
            if (ret.getExpression().isPresent()) {
                // return 带返回值
                IRVirtualRegister r = expr.build(ret.getExpression().get());
                InstructionFactory.ret(ctx, r);
            } else {
                // return 无返回值
                InstructionFactory.retVoid(ctx);
            }
            return;
        }
        if (stmt instanceof BreakNode) {
            // break 语句：跳转到当前最近一层循环的结束标签
            if (breakTargets.isEmpty()) {
                throw new IllegalStateException("`break` appears outside of a loop");
            }
            InstructionFactory.jmp(ctx, breakTargets.peek());
            return;
        }
        if (stmt instanceof ContinueNode) {
            // continue 语句：跳转到当前最近一层循环的 step 起始标签
            if (continueTargets.isEmpty()) {
                throw new IllegalStateException("`continue` appears outside of a loop");
            }
            InstructionFactory.jmp(ctx, continueTargets.peek());
            return;
        }
        // 不支持的语句类型
        throw new IllegalStateException("Unsupported statement: " + stmt.getClass().getSimpleName() + ": " + stmt);
    }

    /**
     * 获取变量名对应的寄存器，如果尚未声明则新声明一个并返回。
     *
     * @param name 变量名，不能为空
     * @param type 变量类型，不能为空
     * @return 变量对应的虚拟寄存器 {@link IRVirtualRegister}
     */
    private IRVirtualRegister getOrDeclareRegister(String name, String type) {
        IRVirtualRegister reg = ctx.getScope().lookup(name);
        if (reg == null) {
            reg = ctx.newRegister();
            ctx.getScope().declare(name, type, reg);
        }
        return reg;
    }

    /**
     * 批量构建一组语句节点，按顺序依次处理。
     *
     * @param stmts 语句节点集合，不可为 null
     */
    private void buildStatements(Iterable<StatementNode> stmts) {
        for (StatementNode s : stmts) build(s);
    }

    /**
     * 构建循环语句（for/while），包括初始语句、条件判断、循环体、更新语句、跳回判断等 IR 指令。
     * <p>
     * 自动维护 break/continue 的目标标签。
     * </p>
     *
     * @param loop 循环节点，不能为空
     */
    private void buildLoop(LoopNode loop) {
        if (loop.init() != null) build(loop.init());
        String lblStart = ctx.newLabel();
        String lblEnd = ctx.newLabel();
        // 循环开始标签
        InstructionFactory.label(ctx, lblStart);

        // 条件不满足则跳出循环
        emitConditionalJump(loop.cond(), lblEnd);
        // 在进入循环体前，记录本层循环的结束标签，供 break 使用
        breakTargets.push(lblEnd);
        // 记录本层循环的 step 起始标签，供 continue 使用
        String lblStep = ctx.newLabel();
        continueTargets.push(lblStep);
        try {
            // 构建循环体
            buildStatements(loop.body());
        } finally {
            // 离开循环体时弹出标签，避免影响外层
            breakTargets.pop();
            continueTargets.pop();
        }
        // step 起始标签(所有 continue 会跳到这里)
        InstructionFactory.label(ctx, lblStep);
        if (loop.step() != null) build(loop.step());

        // 回到 cond 检查
        InstructionFactory.jmp(ctx, lblStart);
        // 循环结束标签
        InstructionFactory.label(ctx, lblEnd);
    }

    /**
     * 构建分支语句（if/else）。
     * <p>
     * 包括：条件判断、then 分支、else 分支（可选）、结束标签等。
     * </p>
     *
     * @param ifNode if 语句节点，不能为空
     */
    private void buildIf(IfNode ifNode) {
        String lblElse = ctx.newLabel();
        String lblEnd = ctx.newLabel();
        // 条件不成立则跳转到 else
        emitConditionalJump(ifNode.condition(), lblElse);

        // then 分支
        buildStatements(ifNode.thenBranch());
        // then 分支执行完后直接跳到结束
        InstructionFactory.jmp(ctx, lblEnd);

        // else 分支（可能为空）
        InstructionFactory.label(ctx, lblElse);
        if (ifNode.elseBranch() != null) buildStatements(ifNode.elseBranch());
        // 结束标签
        InstructionFactory.label(ctx, lblEnd);
    }

    /**
     * 发射条件跳转指令：如果 cond 不成立，则跳转到 falseLabel。
     * <p>
     * 对于二元比较表达式，会选择恰当的比较指令。
     * 其他类型表达式，等价于 (cond == 0) 时跳转。
     * </p>
     *
     * @param cond       条件表达式节点，不可为 null
     * @param falseLabel 条件不成立时跳转的标签，不可为 null
     */
    private void emitConditionalJump(ExpressionNode cond, String falseLabel) {
        if (cond instanceof BinaryExpressionNode(
                ExpressionNode left,
                String operator,
                ExpressionNode right,
                NodeContext _
        )
                && ComparisonUtils.isComparisonOperator(operator)) {

            IRVirtualRegister a = expr.build(left);
            IRVirtualRegister b = expr.build(right);

            // 使用适配后位宽正确的比较指令
            IROpCode cmp = ComparisonUtils.cmpOp(ctx.getScope().getVarTypes(), operator, left, right);
            IROpCode falseOp = IROpCodeUtils.invert(cmp);

            InstructionFactory.cmpJump(ctx, falseOp, a, b, falseLabel);
        } else {
            IRVirtualRegister condReg = expr.build(cond);
            IRVirtualRegister zero = InstructionFactory.loadConst(ctx, 0);
            InstructionFactory.cmpJump(ctx, org.jcnc.snow.compiler.ir.core.IROpCode.CMP_IEQ, condReg, zero, falseLabel);
        }
    }

    /**
     * 递归尝试对表达式做常量折叠（constant folding）。
     * <p>
     * 该方法会根据表达式类型进行常量求值：
     * <ul>
     *   <li>如果是数字字面量，解析为 Integer 或 Double。</li>
     *   <li>如果是字符串字面量，直接返回字符串内容。</li>
     *   <li>如果是布尔字面量，返回 1（true）或 0（false）。</li>
     *   <li>如果是数组字面量，会递归所有元素全部是常量时，返回包含常量元素的 List。</li>
     *   <li>如果是标识符节点，尝试从作用域查找是否登记为常量，如果找到则返回。</li>
     *   <li>其余类型或无法确定常量时，返回 null。</li>
     * </ul>
     * 用于全局/局部常量传播优化与类型推断。
     *
     * @param expr 待折叠的表达式节点，允许为 null
     * @return 如果可折叠则返回其常量值（如 Integer、Double、String、List），否则返回 null
     */
    private Object tryFoldConst(ExpressionNode expr) {
        // 1. 空节点直接返回 null
        if (expr == null) return null;

        // 2. 数字字面量：尝试解析为 Integer 或 Double
        if (expr instanceof NumberLiteralNode n) {
            String s = n.value(); // 获取文本内容
            try {
                // 判断是否为浮点型（包含 . 或 e/E 科学计数法）
                if (s.contains(".") || s.contains("e") || s.contains("E")) {
                    return Double.parseDouble(s); // 解析为 Double
                }
                return Integer.parseInt(s); // 否则解析为 Integer
            } catch (NumberFormatException e) {
                // 解析失败，返回 null
                return null;
            }
        }

        // 3. 字符串字面量：直接返回字符串内容
        if (expr instanceof StringLiteralNode s) {
            return s.value();
        }

        // 4. 布尔字面量：true 返回 1，false 返回 0
        if (expr instanceof BoolLiteralNode b) {
            return b.getValue() ? 1 : 0;
        }

        // 5. 数组字面量：递归所有元素做常量折叠，只有全为常量时才返回 List
        if (expr instanceof ArrayLiteralNode arr) {
            java.util.List<Object> list = new java.util.ArrayList<>();
            for (ExpressionNode e : arr.elements()) {
                Object v = tryFoldConst(e); // 递归折叠每个元素
                if (v == null) return null; // 只要有一个不是常量，则整个数组不是常量
                list.add(v);
            }
            // 所有元素均为常量，返回只读 List
            return java.util.List.copyOf(list);
        }

        // 6. 标识符：尝试查找该变量在当前作用域是否登记为常量
        if (expr instanceof IdentifierNode id) {
            try {
                Object v = ctx.getScope().getConstValue(id.name());
                if (v != null) return v; // 查到常量则返回
            } catch (Throwable ignored) {
            }
        }

        // 7. 其他情况均视为不可折叠，返回 null
        return null;
    }

}
