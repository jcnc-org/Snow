package org.jcnc.snow.vm.commands.flow.control;

import org.jcnc.snow.common.Mode;
import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.*;

import static org.jcnc.snow.common.SnowConfig.print;

/**
 * The CallCommand class implements the {@link Command} interface and represents a subroutine/function call
 * instruction in the virtual machine.
 * <p>
 * This command facilitates method invocation by creating a new stack frame, transferring arguments
 * from the operand stack to the callee's local variable store, and jumping to the specified target address.
 * </p>
 *
 * <p>Specific behavior:</p>
 * <ul>
 *     <li>Parses the target address and the number of arguments from the instruction parameters.</li>
 *     <li>Validates the operands and checks for correct argument count.</li>
 *     <li>Builds a new local variable store for the callee and transfers arguments from the operand stack
 *         (left-to-right order, where the top of the stack is the last argument).</li>
 *     <li>Pushes a new stack frame onto the call stack, saving the return address and local variables.</li>
 *     <li>Jumps to the specified target address to begin execution of the callee function.</li>
 *     <li>If any error occurs (e.g., malformed operands, stack underflow), an exception is thrown.</li>
 * </ul>
 */
public class CallCommand implements Command {

    /**
     * Executes the CALL instruction, initiating a subroutine/function call within the virtual machine.
     * <p>
     * This method handles the creation of a new stack frame for the callee, argument passing,
     * and control transfer to the target function address.
     * </p>
     *
     * @param parts        The instruction parameters. Must include:
     *                     <ul>
     *                         <li>{@code parts[0]}: The "CALL" operator.</li>
     *                         <li>{@code parts[1]}: The target address of the callee function.</li>
     *                         <li>{@code parts[2]}: The number of arguments to pass.</li>
     *                     </ul>
     * @param currentPC    The current program counter, used to record the return address for after the call.
     * @param operandStack The operand stack manager. Arguments are popped from this stack.
     * @param callerLVS    The local variable store of the caller function (not directly modified here).
     * @param callStack    The virtual machine's call stack manager, used to push the new stack frame.
     * @return The new program counter value, which is the address of the callee function (i.e., jump target).
     * The VM should transfer control to this address after setting up the call frame.
     * @throws IllegalArgumentException If the instruction parameters are malformed or missing.
     * @throws IllegalStateException    If the operand stack does not contain enough arguments.
     */
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
            nArgs = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("CALL: malformed operands", e);
        }

        /* build new frame & local table for callee */
        LocalVariableStore calleeLVS = new LocalVariableStore();

        /* transfer arguments: operand stack top is last arg */
        for (int slot = nArgs - 1; slot >= 0; slot--) {
            if (operandStack.isEmpty())
                throw new IllegalStateException("CALL: operand stack underflow");
            calleeLVS.setVariable(slot, operandStack.pop());
        }

        StackFrame newFrame = new StackFrame(currentPC + 1, calleeLVS,
                new MethodContext("subroutine@" + targetAddr, null));
        callStack.pushFrame(newFrame);

        print("\nCalling function at address: " + targetAddr);
        return targetAddr;   // jump
    }
}
