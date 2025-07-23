package org.jcnc.snow.vm.commands.system.control;

import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.utils.LoggingUtils;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * The HaltCommand class implements the {@link Command} interface and represents the stop instruction in the virtual machine.
 * This class is used to terminate the execution of the virtual machine. When this command is executed, the virtual machine
 * outputs a message and stops executing subsequent instructions.
 *
 * <p>Specific behavior:</p>
 * <ul>
 *     <li>Outputs the message "Process has ended," indicating that the program execution has been terminated.</li>
 *     <li>Returns -1 to signal the virtual machine to stop execution.</li>
 * </ul>
 */
public class HaltCommand implements Command {
    /**
     * Default constructor for creating an instance of HaltCommand.
     * This constructor is empty as no specific initialization is required.
     */
    public HaltCommand() {
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
        // Output the termination message
        LoggingUtils.logInfo("\nProcess has ended", "");

        // Return -1 to indicate the program termination, and the virtual machine will not continue executing subsequent instructions
        return -1;
    }
}
