package org.jcnc.snow.vm.commands.function;

import org.jcnc.snow.vm.engine.VMMode;
import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.*;

/**
 * CALL addr nArgs — pushes a new stack-frame, transfers the specified
 * argument count from the operand stack into the callee’s local slots
 * 0‥n-1 (left-to-right), then jumps to {@code addr}.
 */
public class CallCommand implements Command {

    @Override
    public int execute(String[] parts,
                       int currentPC,
                       OperandStack operandStack,
                       LocalVariableStore callerLVS,
                       CallStack callStack) {

        if (parts.length < 3)
            throw new IllegalArgumentException("CALL: need <addr> <nArgs>");

        int targetAddr;
        int nArgs;
        try {
            targetAddr = Integer.parseInt(parts[1]);
            nArgs      = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("CALL: malformed operands", e);
        }

        /* build new frame & local table for callee */
        LocalVariableStore calleeLVS = new LocalVariableStore(VMMode.RUN);

        /* transfer arguments: operand stack top is last arg */
        for (int slot = nArgs - 1; slot >= 0; slot--) {
            if (operandStack.isEmpty())
                throw new IllegalStateException("CALL: operand stack underflow");
            calleeLVS.setVariable(slot, operandStack.pop());
        }

        StackFrame newFrame = new StackFrame(currentPC + 1, calleeLVS,
                new MethodContext("subroutine@" + targetAddr, null));
        callStack.pushFrame(newFrame);

        System.out.println("Calling function at address: " + targetAddr);
        return targetAddr;   // jump
    }
}
