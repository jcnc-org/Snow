package org.jcnc.snow.compiler.ir.builder.handlers;

import org.jcnc.snow.compiler.ir.builder.core.IRBuilderScope;
import org.jcnc.snow.compiler.ir.builder.expression.ExpressionBuilder;
import org.jcnc.snow.compiler.ir.builder.expression.ExpressionHandler;
import org.jcnc.snow.compiler.ir.core.IRValue;
import org.jcnc.snow.compiler.ir.instruction.CallInstruction;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.compiler.parser.ast.CallExpressionNode;
import org.jcnc.snow.compiler.parser.ast.IdentifierNode;
import org.jcnc.snow.compiler.parser.ast.MemberExpressionNode;
import org.jcnc.snow.compiler.parser.ast.base.ExpressionNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 函数/方法调用表达式处理器。
 * <p>
 * 负责将函数调用（CallExpressionNode）转换为 IR 指令，
 * 支持普通函数调用、对象方法调用、super 调用等多种情形的分派和参数处理。
 */
public class CallHandler implements ExpressionHandler<CallExpressionNode> {
    /**
     * 处理调用表达式，生成 IR 调用指令，并返回结果寄存器。
     *
     * @param b    表达式构建器
     * @param call 调用表达式 AST 节点
     * @return 存放调用结果的虚拟寄存器
     */
    @Override
    public IRVirtualRegister handle(ExpressionBuilder b, CallExpressionNode call) {
        // 1. 先将所有参数表达式转换为寄存器（先求值所有参数）
        List<IRVirtualRegister> explicitRegs = new ArrayList<>();
        for (ExpressionNode arg : call.arguments()) {
            explicitRegs.add(b.build(arg));
        }

        String callee;              // 目标函数/方法名
        List<IRValue> finalArgs = new ArrayList<>(); // 实际传递给 IR 的参数表

        // 2. 区分不同类型的 callee 目标
        switch (call.callee()) {

            // super(...) 形式的构造器调用
            case IdentifierNode id when "super".equals(id.name()) -> {
                // 只能在构造函数内部使用
                String thisType = b.ctx().getScope().lookupType("this");
                if (thisType == null)
                    throw new IllegalStateException("super(...) 只能在构造函数中使用");
                String parent = IRBuilderScope.getStructParent(thisType);
                if (parent == null)
                    throw new IllegalStateException(thisType + " 没有父类，无法调用 super(...)");

                // 构造父类构造函数调用
                callee = parent + ".__init__" + explicitRegs.size();
                IRVirtualRegister thisReg = b.ctx().getScope().lookup("this");
                if (thisReg == null)
                    throw new IllegalStateException("未绑定 this");
                // 构造函数第一个参数为 this，后跟实际参数
                finalArgs.add(thisReg);
                finalArgs.addAll(explicitRegs);
            }

            // super.method(...) 或 recv.method(...) 形式
            case MemberExpressionNode m when m.object() instanceof IdentifierNode idObj -> {
                String recvName = idObj.name();
                if ("super".equals(recvName)) {
                    // super.method(...)
                    String thisType = b.ctx().getScope().lookupType("this");
                    if (thisType == null)
                        throw new IllegalStateException("super.method(...) 只能在实例方法中使用");
                    String parent = IRBuilderScope.getStructParent(thisType);
                    if (parent == null)
                        throw new IllegalStateException(thisType + " 没有父类");

                    callee = parent + "." + m.member();
                    IRVirtualRegister thisReg = b.ctx().getScope().lookup("this");
                    if (thisReg == null)
                        throw new IllegalStateException("未绑定 this");
                    // 调用父类方法，第一个参数为 this
                    finalArgs.add(thisReg);
                    finalArgs.addAll(explicitRegs);

                    // 追加 _N 后缀（N=总参数，包括this）
                    callee = callee + "_" + finalArgs.size();
                } else {
                    // 常规对象方法调用 recv.method(...)
                    String recvType = b.ctx().getScope().lookupType(recvName);
                    if (recvType == null || recvType.isEmpty()) {
                        // 静态/未知类型方法，直接按符号名拼接
                        callee = recvName + "." + m.member();
                        finalArgs.addAll(explicitRegs);
                    } else {
                        // 有类型信息，先查 this 寄存器，再拼方法名
                        callee = recvType + "." + m.member();
                        IRVirtualRegister thisReg = b.ctx().getScope().lookup(recvName);
                        if (thisReg == null)
                            throw new IllegalStateException("Undefined identifier: " + recvName);
                        finalArgs.add(thisReg);
                        finalArgs.addAll(explicitRegs);

                        //  追加 _N 后缀（N=总参数，包括this）
                        callee = callee + "_" + finalArgs.size();
                    }
                }
            }

            // 其它表达式对象的方法调用，形如 expr.method(...)
            case MemberExpressionNode m -> {
                // 递归求值 object 部分
                IRVirtualRegister objReg = b.build(m.object());
                callee = m.member();
                // 第一个参数是对象寄存器
                finalArgs.add(objReg);
                finalArgs.addAll(explicitRegs);

                // 追加 _N 后缀（N=总参数，包括this
                callee = callee + "_" + finalArgs.size();
            }

            // 普通函数调用/本地方法调用 foo(...)
            case IdentifierNode id -> {
                // 支持当前作用域内的方法局部重写（如 A.foo -> foo）
                String current = b.ctx().getFunction().name();
                int dot = current.lastIndexOf('.');
                if (dot >= 0 && !id.name().contains(".")) {
                    // 自动补齐作用域前缀
                    callee = current.substring(0, dot) + "." + id.name();
                } else {
                    callee = id.name();
                }
                finalArgs.addAll(explicitRegs);
                // 普通函数无需追加 _N
            }

            // 其它类型均视为不支持
            default -> throw new IllegalStateException(
                    "Unsupported callee type: " + call.callee().getClass().getSimpleName());
        }

        // 3. 分配结果寄存器并生成调用指令
        IRVirtualRegister dest = b.ctx().newRegister();
        b.ctx().addInstruction(new CallInstruction(dest, callee, finalArgs));
        return dest;
    }
}
