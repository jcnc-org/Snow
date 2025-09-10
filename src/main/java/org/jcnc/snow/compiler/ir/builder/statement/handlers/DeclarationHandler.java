package org.jcnc.snow.compiler.ir.builder.statement.handlers;

import org.jcnc.snow.compiler.ir.builder.core.IRBuilderScope;
import org.jcnc.snow.compiler.ir.builder.statement.IStatementHandler;
import org.jcnc.snow.compiler.ir.builder.statement.StatementBuilderContext;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.compiler.parser.ast.DeclarationNode;
import org.jcnc.snow.compiler.parser.ast.NewExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;

/**
 * 变量声明语句处理器（Declaration Statement Handler）。
 * <p>
 * 负责将 AST 层的 {@link DeclarationNode}（如 {@code int a = 1;} 或 {@code Person p = new Student(...);}）
 * 编译为中间表示（IR）的变量声明、初始化、类型绑定、作用域注册等。
 * 支持带初始化器和不带初始化器两种声明，并自动实现常量传播优化及多态类型推导。
 * </p>
 * 多态类型推导的核心实现:
 * <p>
 * 针对如 {@code declare x: Base = new Sub(...);} 这种父类变量直接赋 new 子类对象的情形，
 * 若 {@code Sub} 确实继承自 {@code Base}，则在当前作用域内注册变量时，<b>将类型由 Base 升级为 Sub</b>。
 * 这样后续所有基于该变量的成员方法调用（如 x.getName()）都能静态绑定到子类重写实现（静态多态）。
 * </p>
 * 处理流程:
 * <ol>
 *     <li>如有初始化器，先记录变量声明类型并分配寄存器，生成初始化表达式指令。</li>
 *     <li>尝试常量折叠，注册或清理常量绑定。</li>
 *     <li>如初始化器为 new 子类(...)，且子类继承自声明类型，则变量类型提升为子类。</li>
 *     <li>最终在作用域注册变量名、类型（可能提升）、寄存器绑定。</li>
 *     <li>如无初始化器，仅做类型和符号表注册，并清空常量绑定。</li>
 * </ol>
 * <p>
 * 注意事项:
 * <ul>
 *     <li>本实现不会影响已有运行时结构，适用于现有虚拟机/IR，不依赖对象 vtable 派发。</li>
 *     <li>如需支持运行时真正多态，请在对象结构及方法调用指令层面额外设计。</li>
 * </ul>
 */
public class DeclarationHandler implements IStatementHandler {

    /**
     * 判断类型 child 是否是 parent 的子类（包含多级继承）。
     * 依赖 IRBuilderScope 注册的 STRUCT_PARENTS。
     *
     * @param child  子类名
     * @param parent 父类名
     * @return 若 child 继承自 parent，返回 true，否则 false
     */
    private static boolean isSubclassOf(String child, String parent) {
        if (child == null || parent == null) return false;
        if (child.equals(parent)) return true;
        String t = child;
        while (t != null) {
            if (t.equals(parent)) return true;
            t = IRBuilderScope.getStructParent(t);
        }
        return false;
    }

    /**
     * 判断当前 Handler 是否可处理给定 AST 语句节点。
     *
     * @param stmt AST 语句节点
     * @return 若为 DeclarationNode 类型则返回 true，否则返回 false
     */
    @Override
    public boolean canHandle(StatementNode stmt) {
        return stmt instanceof DeclarationNode;
    }

    /**
     * 处理变量声明语句（含类型提升及常量传播）。
     *
     * @param stmt AST 语句节点（已保证为 DeclarationNode 类型）
     * @param c    语句构建上下文
     */
    @Override
    public void handle(StatementNode stmt, StatementBuilderContext c) {
        DeclarationNode decl = (DeclarationNode) stmt;

        if (decl.getInitializer().isPresent()) {
            ExpressionNode init = decl.getInitializer().get();

            // 1. 临时记录变量声明类型，供初始化表达式类型推断
            c.ctx().setVarType(decl.getType());

            // 2. 分配目标寄存器，并生成初始化表达式代码
            IRVirtualRegister dest = c.ctx().newRegister();
            c.expr().buildInto(init, dest);

            // 3. 常量传播优化
            try {
                Object constVal = c.constFolder().tryFoldConst(init);
                if (constVal != null) {
                    c.ctx().getScope().setConstValue(decl.getName(), constVal);
                } else {
                    c.ctx().getScope().clearConstValue(decl.getName());
                }
            } catch (Throwable ignored) {
            }
            c.ctx().clearVarType();

            // 4. 类型提升判定
            String effectiveType = decl.getType();
            if (init instanceof NewExpressionNode newExp) {
                String newType = newExp.typeName();
                if (isSubclassOf(newType, effectiveType)) {
                    effectiveType = newType;
                }
            }

            // 5. 在作用域注册变量名、类型、寄存器
            c.ctx().getScope().declare(decl.getName(), effectiveType, dest);

        } else {
            // 无初始化器：仅注册类型及符号，并清空常量绑定
            c.ctx().getScope().declare(decl.getName(), decl.getType());
            c.ctx().getScope().clearConstValue(decl.getName());
        }
    }
}
