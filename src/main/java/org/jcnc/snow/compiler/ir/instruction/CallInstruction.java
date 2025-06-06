package org.jcnc.snow.compiler.ir.instruction;

import org.jcnc.snow.compiler.ir.core.IRInstruction;
import org.jcnc.snow.compiler.ir.core.IROpCode;
import org.jcnc.snow.compiler.ir.core.IRVisitor;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.compiler.ir.core.IRValue;

import java.util.ArrayList;
import java.util.List;

/**
 * CallInstruction —— 表示一次函数调用，格式：dest = CALL functionName, arg1, arg2, ...
 */
public class CallInstruction extends IRInstruction {
    private final IRVirtualRegister dest;
    private final String functionName;
    private final List<IRValue> arguments;

    public CallInstruction(IRVirtualRegister dest, String functionName, List<IRValue> args) {
        this.dest = dest;
        this.functionName = functionName;
        this.arguments = List.copyOf(args);
    }

    @Override
    public IROpCode op() {
        return IROpCode.CALL;
    }

    @Override
    public List<IRValue> operands() {
        List<IRValue> ops = new ArrayList<>();
        ops.add(dest);
        ops.addAll(arguments);
        return ops;
    }

    public IRVirtualRegister getDest() {
        return dest;
    }

    public String getFunctionName() {
        return functionName;
    }

    public List<IRValue> getArguments() {
        return arguments;
    }

    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(dest + " = CALL " + functionName);
        for (IRValue arg : arguments) {
            sb.append(", ").append(arg);
        }
        return sb.toString();
    }
}
