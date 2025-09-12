package org.jcnc.snow.vm.commands.flow.control;

import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.*;

import static org.jcnc.snow.common.SnowConfig.print;

/**
 * Implements the CALL instruction for the virtual machine.
 *
 * <p>
 * Encoding (by {@code VMProgramBuilder.emitCall}):
 * <ul>
 *     <li>{@code CALL <addr> <nArgs>} — static/known target</li>
 * </ul>
 * The arguments are pushed to the operand stack from left to right before the call,
 * so the last argument is on the top of the stack when this instruction executes.
 * </p>
 */
public class CallCommand implements Command {

    /**
     * Executes the CALL instruction.
     *
     * <p>
     * The CALL instruction transfers control to a subroutine at the given address,
     * passing arguments from the operand stack to a new local variable store.
     * </p>
     *
     * @param parts            the parts of the instruction; expects: [CALL, targetAddress, nArgs]
     * @param currentPC        the current program counter (PC)
     * @param operandStack     the VM's operand stack (used for passing arguments)
     * @param ignoredCallerLVS the local variable store of the caller (unused in this implementation)
     * @param callStack        the VM's call stack (where new frames are pushed)
     * @return the target address to transfer control to (i.e., jump to)
     * @throws IllegalStateException if the instruction format is invalid
     */
    @Override
    public int execute(String[] parts,
                       int currentPC,
                       OperandStack operandStack,
                       LocalVariableStore /* caller LVT, unused */ ignoredCallerLVS,
                       CallStack callStack) {

        // 1. Validate instruction arguments.
        if (parts.length < 3) {
            throw new IllegalStateException("CALL requires 2 operands: target and nArgs");
        }

        final String rawTarget = parts[1].trim();
        final int nArgs = Integer.parseInt(parts[2].trim());

        // 2. Pop arguments from the operand stack and restore left-to-right order.
        //    Arguments are pushed left-to-right, so we pop them and reverse into the args array.
        final Object[] args = new Object[nArgs];
        for (int i = nArgs - 1; i >= 0; i--) {
            args[i] = operandStack.pop();
        }

        // 3. Resolve the target address for the subroutine (currently supports only static calls).
        int targetAddr = Integer.parseInt(rawTarget);
        String methodNameForCtx = "subroutine@" + targetAddr;
//        print("\nCALL -> " + targetAddr);

        // 4. Build the callee's local variable store and copy arguments into it.
        LocalVariableStore calleeLVS = new LocalVariableStore();
        for (int i = 0; i < nArgs; i++) {
            calleeLVS.setVariable(i, args[i]);
        }

        // 5. Create a new stack frame for the callee and push it onto the call stack.
        //    The return address is set to the next instruction after CALL.
        StackFrame newFrame = new StackFrame(
                currentPC + 1,
                calleeLVS,
                new MethodContext(methodNameForCtx, null) // Don't log full args to avoid heavy logs
        );
        callStack.pushFrame(newFrame);

        // 6. Transfer control to the target address (subroutine entry point).
        return targetAddr;
    }
}
