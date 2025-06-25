package org.jcnc.snow.vm.commands.control.int32;

import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.utils.LoggingUtils;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * The ICECommand class implements the {@link Command} interface and represents a conditional jump command in the virtual machine.
 * This class compares two values from the stack, and if they are equal, it jumps to the specified target command.
 *
 * <p>Specific behavior:</p>
 * <ul>
 *     <li>Pops two int32 from the virtual machine stack.</li>
 *     <li>If the two int32 are equal, jumps to the target command.</li>
 *     <li>Otherwise, the program continues with the next command.</li>
 * </ul>
 */
public class ICECommand implements Command {
    /**
     * Default constructor for creating an instance of ICECommand.
     * This constructor is empty as no specific initialization is required.
     */
    public ICECommand() {
        // Empty constructor
    }

    /**
     * Executes the virtual machine instruction's operation.
     *
     * <p>This method retrieves the necessary data from the virtual machine stack and local variable store based on the instruction's
     * specific implementation, performs the operation, and updates the program counter (PC) to reflect the next instruction
     * to be executed.</p>
     *
     * <p>The parameters provided allow the command to manipulate the operand stack, modify the local variables, and control the flow
     * of execution by updating the program counter.</p>
     *
     * <p>The exact behavior of this method will depend on the specific instruction being executed (e.g., arithmetic, branching,
     * function calls, etc.). For example, a `CALL` instruction will modify the call stack by pushing a new frame,
     * while a `POP` instruction will remove an item from the operand stack.</p>
     *
     * @param parts              The array of instruction parameters, which usually includes the operator and related arguments
     *                           (such as target addresses, values, or function names). These parameters may vary based on
     *                           the instruction being executed.
     * @param currentPC          The current program counter-value, indicating the address of the instruction being executed.
     *                           This value is typically incremented after the execution of each instruction to point to the next one.
     * @param operandStack       The virtual machine's operand stack manager, responsible for performing operations on the operand stack,
     *                           such as pushing, popping, and peeking values.
     * @param localVariableStore The local variable store, typically used to manage method-local variables during instruction execution.
     *                           The store may not be used in every command but can be leveraged by instructions that require access
     *                           to local variables.
     * @param callStack          The virtual machine's call stack, which keeps track of the method invocation hierarchy. It is used by
     *                           instructions that involve method calls or returns (such as `CALL` and `RETURN` instructions).
     * @return The updated program counter-value, typically the current program counter-value incremented by 1, unless the
     * instruction modifies control flow (such as a `JUMP` or `CALL`), in which case it may return a new address
     * corresponding to the target of the jump or the subroutine to call.
     */
    @Override
    public int execute(String[] parts, int currentPC, OperandStack operandStack, LocalVariableStore localVariableStore, CallStack callStack) {
        // Parse the target command address
        int target = Integer.parseInt(parts[1]);

        // Pop the two operands from the stack
        int b = (int) operandStack.pop();
        int a = (int) operandStack.pop();

        // If the operands are equal, jump to the target command
        if (a == b) {
            LoggingUtils.logInfo("Jumping to command", String.valueOf(target));
            return target;
        }

        return currentPC + 1;
    }
}