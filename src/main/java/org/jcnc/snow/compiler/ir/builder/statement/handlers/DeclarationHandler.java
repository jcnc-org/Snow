package org.jcnc.snow.compiler.ir.builder.statement.handlers;

import org.jcnc.snow.compiler.ir.builder.statement.IStatementHandler;
import org.jcnc.snow.compiler.ir.builder.statement.StatementBuilderContext;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.compiler.parser.ast.DeclarationNode;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;

/**
 * 变量声明语句处理器。
 * <p>
 * 负责将AST中的DeclarationNode节点（如“int a = 1;”）编译为IR层的变量声明和初始化指令。
 * 支持带初始化器与不带初始化器两种情况，并自动管理常量传播和作用域绑定。
 * </p>
 */
public class DeclarationHandler implements IStatementHandler {
    /**
     * 判断是否可以处理给定的语句节点。
     *
     * @param stmt AST语句节点
     * @return 若为DeclarationNode类型则返回true，否则返回false
     */
    @Override
    public boolean canHandle(StatementNode stmt) {
        return stmt instanceof DeclarationNode;
    }

    /**
     * 处理变量声明节点，生成变量声明与初始化的IR指令。
     * <p>
     * 处理流程：
     * <ol>
     *   <li>若有初始化器，先记录类型，生成目标寄存器，并对初始化表达式赋值。</li>
     *   <li>尝试常量折叠，若为常量则注册为常量变量，否则清除常量绑定。</li>
     *   <li>完成变量声明并注册到作用域。</li>
     *   <li>无初始化器时只做类型和符号表注册，并清空常量绑定。</li>
     * </ol>
     * </p>
     *
     * @param stmt AST语句节点（已保证为DeclarationNode类型）
     * @param c    语句构建上下文
     */
    @Override
    public void handle(StatementNode stmt, StatementBuilderContext c) {
        DeclarationNode decl = (DeclarationNode) stmt;

        if (decl.getInitializer().isPresent()) {
            // 1. 有初始化器：先临时设置类型，分配目标寄存器并生成赋值IR
            c.ctx().setVarType(decl.getType());
            IRVirtualRegister dest = c.ctx().newRegister();
            c.expr().buildInto(decl.getInitializer().get(), dest);

            try {
                // 2. 尝试常量折叠，做常量传播优化
                Object constVal = c.constFolder().tryFoldConst(decl.getInitializer().get());
                if (constVal != null) c.ctx().getScope().setConstValue(decl.getName(), constVal);
                else c.ctx().getScope().clearConstValue(decl.getName());
            } catch (Throwable ignored) {
            }

            c.ctx().clearVarType();
            // 3. 注册变量到作用域，绑定寄存器
            c.ctx().getScope().declare(decl.getName(), decl.getType(), dest);
        } else {
            // 4. 无初始化器，仅注册变量并清空常量绑定
            c.ctx().getScope().declare(decl.getName(), decl.getType());
            c.ctx().getScope().clearConstValue(decl.getName());
        }
    }
}
