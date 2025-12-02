package org.jcnc.snow.compiler.ir.builder.statement.handlers;

import org.jcnc.snow.compiler.ir.builder.core.IRBuilderScope;
import org.jcnc.snow.compiler.ir.builder.core.InstructionFactory;
import org.jcnc.snow.compiler.ir.builder.statement.IStatementHandler;
import org.jcnc.snow.compiler.ir.builder.statement.StatementBuilderContext;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.compiler.parser.ast.AssignmentNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.NodeContext;
import org.jcnc.snow.compiler.parser.ast.base.StatementNode;

import java.util.Locale;
import java.util.Map;

/**
 * 普通赋值语句处理器。
 * <p>
 * 负责将AST中的AssignmentNode节点（如“a = b + 1”）编译为IR层的变量/成员/新变量赋值指令。
 * 支持本地变量赋值、结构体成员赋值（支持this.<field>）、以及自动声明赋值等场景。
 * 并自动尝试常量折叠与绑定。
 * </p>
 */
public class AssignmentHandler implements IStatementHandler {

    /**
     * 判断是否可以处理给定的语句节点。
     *
     * @param stmt AST语句节点
     * @return 若为AssignmentNode类型则返回true，否则返回false
     */
    @Override
    public boolean canHandle(StatementNode stmt) {
        return stmt instanceof AssignmentNode;
    }

    /**
     * 处理赋值节点，生成变量赋值或结构体成员赋值等IR指令。
     * <p>
     * 处理流程：
     * <ol>
     *   <li>优先查找本地变量寄存器，若存在则直接赋值。</li>
     *   <li>若为this.<field>赋值，则查找结构体布局与成员下标，生成结构体成员赋值。</li>
     *   <li>否则自动声明变量并赋值（支持全新变量）。</li>
     *   <li>全部流程都尝试常量折叠与绑定，失败则清除常量绑定。</li>
     * </ol>
     * </p>
     *
     * @param stmt AST语句节点（已保证为AssignmentNode类型）
     * @param c    语句构建上下文
     */
    @Override
    public void handle(StatementNode stmt, StatementBuilderContext c) {
        if (!(stmt instanceof AssignmentNode(String var, ExpressionNode rhs, NodeContext nodeContext))) {
            throw new IllegalStateException("Unexpected AssignmentNode pattern");
        }

        final String type = c.ctx().getScope().lookupType(var);
        IRVirtualRegister localReg = c.ctx().getScope().lookup(var);
        if (localReg != null) {
            // 1. 本地变量赋值
            c.ctx().setVarType(type);
            c.expr().buildInto(rhs, localReg);

            try {
                Object constVal = c.constFolder().tryFoldConst(rhs);
                if (constVal != null) c.ctx().getScope().setConstValue(var, constVal);
                else c.ctx().getScope().clearConstValue(var);
            } catch (Throwable ignored) {
            }
            c.ctx().clearVarType();
            return;
        }

        // 2. 尝试 this.<field> 赋值（结构体/类成员变量）
        IRVirtualRegister thisReg = c.ctx().getScope().lookup("this");
        String thisType = c.ctx().getScope().lookupType("this");
        if (thisReg != null && thisType != null) {
            Map<String, Integer> layout = IRBuilderScope.getStructLayout(thisType);
            if (layout == null) {
                int dot = thisType.lastIndexOf('.');
                if (dot >= 0 && dot + 1 < thisType.length()) {
                    layout = IRBuilderScope.getStructLayout(thisType.substring(dot + 1));
                }
            }
            Integer idx = (layout != null) ? layout.get(var) : null;
            if (idx != null) {
                String fieldType = IRBuilderScope.getStructFieldType(thisType, var);
                if (fieldType != null) {
                    c.ctx().setVarType(fieldType);
                }
                IRVirtualRegister valReg = c.expr().build(rhs);
                IRVirtualRegister idxReg = InstructionFactory.loadConst(c.ctx(), idx);
                java.util.List<org.jcnc.snow.compiler.ir.core.IRValue> argv =
                        java.util.List.of(thisReg, idxReg, valReg);
                String setFn = selectSetIndexFunc(fieldType);
                c.ctx().addInstruction(new org.jcnc.snow.compiler.ir.instruction.CallInstruction(
                        null, setFn, argv));

                try {
                    c.ctx().getScope().clearConstValue(var);
                } catch (Throwable ignored) {
                }
                c.ctx().clearVarType();
                return;
            }
        }

        // 3. 新声明并赋值（自动声明局部变量，或支持动态类型等场景）
        c.ctx().setVarType(type);
        IRVirtualRegister target = getOrDeclareRegister(c, var, type);
        c.expr().buildInto(rhs, target);

        try {
            Object constVal = c.constFolder().tryFoldConst(rhs);
            if (constVal != null) c.ctx().getScope().setConstValue(var, constVal);
            else c.ctx().getScope().clearConstValue(var);
        } catch (Throwable ignored) {
        }
        c.ctx().clearVarType();
    }

    /**
     * 获取已声明的变量寄存器，若不存在则声明新变量并分配寄存器。
     *
     * @param c    语句构建上下文
     * @param name 变量名
     * @param type 变量类型
     * @return IR虚拟寄存器
     */
    private IRVirtualRegister getOrDeclareRegister(StatementBuilderContext c, String name, String type) {
        IRVirtualRegister reg = c.ctx().getScope().lookup(name);
        if (reg == null) {
            reg = c.ctx().newRegister();
            c.ctx().getScope().declare(name, type, reg);
        }
        return reg;
    }

    /**
     * 根据字段类型选择合适的 __setindex_* 通道。
     */
    private String selectSetIndexFunc(String fieldType) {
        if (fieldType == null || fieldType.isBlank()) return "__setindex_r";
        return switch (fieldType.toLowerCase(Locale.ROOT)) {
            case "byte" -> "__setindex_b";
            case "short" -> "__setindex_s";
            case "int", "integer", "bool", "boolean" -> "__setindex_i";
            case "long" -> "__setindex_l";
            case "float" -> "__setindex_f";
            case "double" -> "__setindex_d";
            default -> "__setindex_r";
        };
    }
}
