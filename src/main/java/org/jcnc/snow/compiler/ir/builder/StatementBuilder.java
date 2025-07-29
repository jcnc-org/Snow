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
import java.util.Locale;

/**
 * StatementBuilder —— 将 AST 语句节点 ({@link StatementNode}) 转换为 IR 指令序列的构建器。
 * <p>
 * 负责将各种语句节点（循环、分支、表达式、赋值、声明、返回等）生成对应的 IR 指令，并管理作用域和控制流标签。
 * </p>
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
     * break 目标标签栈（保存每层循环的结束标签）
     */
    private final ArrayDeque<String> breakTargets = new ArrayDeque<>();
    /**
     * continue 目标标签栈（保存每层循环的 step 起始标签）
     */
    private final ArrayDeque<String> continueTargets = new ArrayDeque<>();

    /**
     * 构造方法。
     *
     * @param ctx IR 编译上下文环境
     */
    public StatementBuilder(IRContext ctx) {
        this.ctx = ctx;
        this.expr = new ExpressionBuilder(ctx);
    }

    private static char typeSuffixFromType(String type) {
        if (type == null) return '\0';
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "byte" -> 'b';
            case "short" -> 's';
            case "long" -> 'l';
            case "float" -> 'f';
            case "double" -> 'd';
            default -> '\0';   // 其余默认按 32-bit 整型处理
        };
    }

    /**
     * 将一个 AST 语句节点转为 IR 指令序列。
     * 根据节点类型分发到对应的处理方法。
     *
     * @param stmt 待转换的语句节点
     */
    public void build(StatementNode stmt) {
        if (stmt instanceof LoopNode loop) {
            // 循环语句
            buildLoop(loop);
            return;
        }
        if (stmt instanceof IfNode ifNode) {
            // 分支（if-else）语句
            buildIf(ifNode);
            return;
        }
        if (stmt instanceof ExpressionStatementNode(ExpressionNode exp, NodeContext _)) {
            // 纯表达式语句，如 foo();
            expr.build(exp);
            return;
        }
        if (stmt instanceof AssignmentNode(String var, ExpressionNode rhs, NodeContext _)) {
            // 赋值语句，如 a = b + 1;

            final String type = ctx.getScope().lookupType(var);

            // 1. 设置声明变量的类型
            ctx.setVarType(type);

            IRVirtualRegister target = getOrDeclareRegister(var, type);
            expr.buildInto(rhs, target);

            // 2. 清除变量声明
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

                // 4. 清理类型设置，防止影响后续变量声明
                ctx.clearVarType();

                // 5. 在作用域内将变量名与新分配的寄存器进行绑定
                //    这样后续对该变量的任何操作都只会影响 dest，不会反向影响初值表达式中的源变量
                ctx.getScope().declare(decl.getName(), decl.getType(), dest);
            } else {
                // 仅声明变量，无初值（如 int a;）
                // 在作用域内声明并分配新寄存器，但不进行初始化
                ctx.getScope().declare(decl.getName(), decl.getType());
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
     * 获取变量名对应的寄存器，不存在则声明一个新的。
     *
     * @param name 变量名
     * @return 变量对应的虚拟寄存器
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
     * 批量构建一组语句节点，顺序处理每个语句。
     *
     * @param stmts 语句节点集合
     */
    private void buildStatements(Iterable<StatementNode> stmts) {
        for (StatementNode s : stmts) build(s);
    }

    /**
     * 构建循环语句（for/while）。
     * 处理流程: 初始语句 → 条件判断 → 循环体 → 更新语句 → 跳回条件。
     *
     * @param loop 循环节点
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
     * 处理流程: 条件判断 → then 分支 → else 分支（可选）。
     *
     * @param ifNode if 语句节点
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
     * 条件跳转指令的生成。
     * 如果是二元比较表达式，直接使用对应比较操作码；否则等价于与 0 比较。
     *
     * @param cond       条件表达式
     * @param falseLabel 条件不成立时跳转到的标签
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
            InstructionFactory.cmpJump(ctx, IROpCode.CMP_IEQ, condReg, zero, falseLabel);
        }
    }
}
