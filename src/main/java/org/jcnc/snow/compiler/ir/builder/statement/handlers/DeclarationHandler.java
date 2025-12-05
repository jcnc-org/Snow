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
 *
 * <p>
 * 本处理器负责将 AST 层的 {@link DeclarationNode}（例如：
 * <ul>
 *     <li><code>int a = 1;</code></li>
 *     <li><code>Person p = new Student(...);</code></li>
 * </ul>
 * 转换为 IR 层的声明、初始化、常量传播、类型判定与作用域注册等内容。
 * </p>
 *
 * <p>
 * 核心功能包括：
 * </p>
 * <ul>
 *     <li>根据声明类型与初始化器生成 IR 代码。</li>
 *     <li>在作用域中注册变量名称、类型信息、寄存器绑定。</li>
 *     <li>支持常量折叠与常量传播，以便在后续优化阶段使用。</li>
 *     <li>支持多态类型提升：当声明类型为父类而初始化表达式为 <code>new 子类</code> 时，自动将变量类型提升为实际子类。</li>
 * </ul>
 *
 * <p>
 * 多态类型提升示例：
 * </p>
 * <pre>{@code
 * declare x: Base = new Sub(...);
 * }</pre>
 * <p>
 * 若 <code>Sub</code> 继承自 <code>Base</code>，则在作用域中将变量 x 的实际类型注册为 <code>Sub</code>。
 * 这样后续针对 x 的所有成员访问都能在编译期静态绑定到子类实现（静态多态）。
 * </p>
 *
 * <p>
 * 整体处理流程：
 * </p>
 * <ol>
 *     <li>若作用域中已存在同名寄存器，则复用并仅执行初始化与类型判定。</li>
 *     <li>若有初始化器：
 *         <ol>
 *             <li>记录声明类型供表达式类型推断使用。</li>
 *             <li>分配寄存器并生成初始化表达式的 IR。</li>
 *             <li>尝试常量折叠，并根据折叠结果更新常量绑定。</li>
 *             <li>若初始化器为 <code>new</code> 子类且符合继承关系，则执行类型提升。</li>
 *             <li>将变量名、有效类型、寄存器绑定注册到作用域中。</li>
 *         </ol>
 *     </li>
 *     <li>若无初始化器，仅注册变量的显式声明类型并清空常量绑定。</li>
 * </ol>
 *
 * <p>
 * 本处理器确保变量声明相关的所有语义在 IR 层均被完整表达，包括寄存器分配、常量值、真实类型（含子类）、符号信息等，
 * 以支持后续的 IR 优化与指令生成阶段。
 * </p>
 */
public class DeclarationHandler implements IStatementHandler {

    /**
     * 判断类型 child 是否为 parent 的子类（包含多级继承）。
     * <p>
     * 会沿着 child → parent 的继承链向上查找，依赖 {@link IRBuilderScope} 的结构体父类注册信息。
     * </p>
     *
     * @param child  子类名称
     * @param parent 父类名称
     * @return 若 child 为 parent 的子类或相同类型，返回 true；否则返回 false
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
     * 判断当前 Handler 是否能够处理指定的 AST 节点。
     *
     * @param stmt AST 语句节点
     * @return 若节点为 {@link DeclarationNode} 则返回 true，否则返回 false
     */
    @Override
    public boolean canHandle(StatementNode stmt) {
        return stmt instanceof DeclarationNode;
    }

    /**
     * 处理变量声明（包含寄存器分配、初始化表达式生成、常量传播与类型提升）。
     *
     * @param stmt AST 声明节点（已保证为 {@link DeclarationNode} 类型）
     * @param c    构建上下文（包含作用域、表达式构建器、寄存器管理等）
     */
    @Override
    public void handle(StatementNode stmt, StatementBuilderContext c) {
        DeclarationNode decl = (DeclarationNode) stmt;

        // 若作用域内已存在同名寄存器（例如全局变量的预注册），则复用处理
        IRVirtualRegister existing = c.ctx().getScope().lookup(decl.getName());
        if (existing != null) {
            String effectiveType = decl.getType();

            if (decl.getInitializer().isPresent()) {
                ExpressionNode init = decl.getInitializer().get();

                // 为初始化表达式提供声明类型信息
                c.ctx().setVarType(effectiveType);
                c.expr().buildInto(init, existing);

                // 常量折叠
                try {
                    Object constVal = c.constFolder().tryFoldConst(init);
                    if (constVal != null) {
                        c.ctx().getScope().setConstValue(decl.getName(), constVal);
                    } else {
                        c.ctx().getScope().clearConstValue(decl.getName());
                    }
                } catch (Throwable ignored) {
                }

                // 类型提升判断
                if (init instanceof NewExpressionNode newExp) {
                    String newType = newExp.typeName();
                    if (isSubclassOf(newType, effectiveType)) {
                        effectiveType = newType;
                    }
                }
                c.ctx().clearVarType();
            } else {
                // 无初始化器时清除常量绑定
                c.ctx().getScope().clearConstValue(decl.getName());
            }

            // 注册变量名 → 有效类型 → 寄存器
            c.ctx().getScope().declare(decl.getName(), effectiveType, existing);
            return;
        }

        // ——无预存在寄存器：执行正常声明流程——
        if (decl.getInitializer().isPresent()) {
            ExpressionNode init = decl.getInitializer().get();

            // 提供声明类型用于类型推断
            c.ctx().setVarType(decl.getType());

            // 分配寄存器并生成初始化器 IR
            IRVirtualRegister dest = c.ctx().newRegister();
            c.expr().buildInto(init, dest);

            // 常量折叠
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

            // 判断是否需要类型提升
            String effectiveType = decl.getType();
            if (init instanceof NewExpressionNode newExp) {
                String newType = newExp.typeName();
                if (isSubclassOf(newType, effectiveType)) {
                    effectiveType = newType;
                }
            }

            // 最终注册变量
            c.ctx().getScope().declare(decl.getName(), effectiveType, dest);

        } else {
            // 无初始化器：仅注册类型与符号
            c.ctx().getScope().declare(decl.getName(), decl.getType());
            c.ctx().getScope().clearConstValue(decl.getName());
        }
    }
}
