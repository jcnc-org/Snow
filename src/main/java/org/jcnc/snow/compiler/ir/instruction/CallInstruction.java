package org.jcnc.snow.compiler.ir.instruction;

import org.jcnc.snow.compiler.ir.core.IRInstruction;
import org.jcnc.snow.compiler.ir.core.IROpCode;
import org.jcnc.snow.compiler.ir.core.IRVisitor;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;
import org.jcnc.snow.compiler.ir.core.IRValue;

import java.util.ArrayList;
import java.util.List;

/**
 * CallInstruction —— 表示一次函数调用的中间代码指令
 * 形式为：dest = CALL functionName, arg1, arg2, ...
 */
public class CallInstruction extends IRInstruction {
    /**
     * 调用结果存放的目标虚拟寄存器
     */
    private final IRVirtualRegister dest;

    /**
     * 被调用的函数名
     */
    private final String functionName;

    /**
     * 传递给函数的参数列表
     */
    private final List<IRValue> arguments;

    /**
     * 构造函数，创建一个函数调用指令
     *
     * @param dest         调用结果存放的目标寄存器
     * @param functionName 被调用的函数名
     * @param args         参数列表
     */
    public CallInstruction(IRVirtualRegister dest, String functionName, List<IRValue> args) {
        this.dest = dest;
        this.functionName = functionName;
        this.arguments = List.copyOf(args);
    }

    /**
     * 获取该指令的操作码
     *
     * @return 操作码 CALL
     */
    @Override
    public IROpCode op() {
        return IROpCode.CALL;
    }

    /**
     * 获取该指令涉及的操作数（目标寄存器和所有参数）
     *
     * @return 操作数列表
     */
    @Override
    public List<IRValue> operands() {
        List<IRValue> ops = new ArrayList<>();
        ops.add(dest);
        ops.addAll(arguments);
        return ops;
    }

    /**
     * 获取目标虚拟寄存器
     *
     * @return 目标虚拟寄存器
     */
    public IRVirtualRegister getDest() {
        return dest;
    }

    /**
     * 获取被调用的函数名
     *
     * @return 函数名
     */
    public String getFunctionName() {
        return functionName;
    }

    /**
     * 获取参数列表
     *
     * @return 参数列表
     */
    public List<IRValue> getArguments() {
        return arguments;
    }

    /**
     * 接受 IRVisitor 访问
     *
     * @param visitor 访问者
     */
    @Override
    public void accept(IRVisitor visitor) {
        visitor.visit(this);
    }

    /**
     * 获取该指令的字符串表示
     *
     * @return 字符串形式
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(dest + " = CALL " + functionName);
        for (IRValue arg : arguments) {
            sb.append(", ").append(arg);
        }
        return sb.toString();
    }
}
