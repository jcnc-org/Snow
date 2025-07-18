package org.jcnc.snow.compiler.ir.instruction;

import org.jcnc.snow.compiler.ir.common.GlobalFunctionTable;
import org.jcnc.snow.compiler.ir.core.IRInstruction;
import org.jcnc.snow.compiler.ir.core.IROpCode;
import org.jcnc.snow.compiler.ir.core.IRValue;
import org.jcnc.snow.compiler.ir.core.IRVisitor;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.ArrayList;
import java.util.List;

/**
 * CallInstruction —— 表示一次函数调用的中间代码指令
 * <pre>
 *   dest = CALL foo, arg1, arg2, ...
 * </pre>
 * 若被调函数返回 void，则 {@code dest} 可以为 {@code null}，
 * 并且不会参与寄存器分配。
 */
public class CallInstruction extends IRInstruction {
    /** 调用结果目标寄存器；void 返回时可为 null */
    private final IRVirtualRegister dest;
    /** 被调用的函数名（含模块限定） */
    private final String functionName;
    /** 实参列表 */
    private final List<IRValue> arguments;

    public CallInstruction(IRVirtualRegister dest,
                           String functionName,
                           List<IRValue> args) {
        this.dest = dest;
        this.functionName = functionName;
        this.arguments = List.copyOf(args);
    }

    // === 基本信息 ===
    @Override
    public IROpCode op() {
        return IROpCode.CALL;
    }

    /** 仅在函数有返回值时才暴露目标寄存器 */
    @Override
    public IRVirtualRegister dest() {
        return isVoidReturn() ? null : dest;
    }

    /** 操作数列表: void 调用不包含 dest */
    @Override
    public List<IRValue> operands() {
        List<IRValue> ops = new ArrayList<>();
        if (!isVoidReturn() && dest != null) {
            ops.add(dest);
        }
        ops.addAll(arguments);
        return ops;
    }

    // === Getter ===
    public IRVirtualRegister getDest() {
        return dest;
    }

    public String getFunctionName() {
        return functionName;
    }

    public List<IRValue> getArguments() {
        return arguments;
    }

    // === 帮助方法 ===
    /** 判断被调函数是否返回 void */
    private boolean isVoidReturn() {
        return "void".equals(GlobalFunctionTable.getReturnType(functionName));
    }

    // === 访客模式 ===
    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }

    // === 字符串表示 ===
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (!isVoidReturn() && dest != null) {
            sb.append(dest).append(" = ");
        }
        sb.append("CALL ").append(functionName);
        for (IRValue arg : arguments) {
            sb.append(", ").append(arg);
        }
        return sb.toString();
    }
}
