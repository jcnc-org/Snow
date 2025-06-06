package org.jcnc.snow.vm.commands.arithmetic.long64;

import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * LIncCommand Opcode: Represents the long64 increment operation for a local variable in the virtual machine.
 * <p>This opcode is implemented by the {@link LIncCommand} class, which defines its specific execution logic.</p>
 *
 * <p>Execution Steps:</p>
 * <ol>
 *     <li>Extracts the index of the local variable and the increment value from the instruction parameters.</li>
 *     <li>Retrieves the current value of the local variable at the given index.</li>
 *     <li>Increments the value of the local variable by the specified increment value (i.e., <code>localVariables[index] += increment</code>).</li>
 *     <li>Updates the local variable with the new incremented value.</li>
 *     <li>Returns the updated program counter (PC) value, which typically increments by 1 unless control flow is modified.</li>
 * </ol>
 *
 * <p>This opcode is useful for optimizing the modification of local variables, especially in tight loops or when managing counters.</p>
 */
public class LIncCommand implements Command {

    /**
     * Default constructor for creating an instance of LIncCommand.
     * This constructor does not perform any specific initialization, as the command's parameters are passed during execution.
     */
    public LIncCommand() {
        // Empty constructor
    }

    /**
     * Executes the virtual machine instruction's operation for incrementing a local variable.
     *
     * <p>This method retrieves the necessary data (index of the local variable and the increment value) from the instruction parameters,
     * performs the increment operation on the specified local variable, and returns the updated program counter (PC) value.</p>
     *
     * @param parts              The array of instruction parameters, which includes the index of the local variable and
     *                           the increment value (passed directly as bytecode parameters).
     * @param currentPC          The current program counter-value, indicating the address of the instruction being executed.
     *                           Typically incremented after execution to point to the next instruction.
     * @param operandStack       The operand stack manager of the virtual machine, responsible for operations such as push, pop, and peek.
     * @param localVariableStore The local variable store, used to manage method-local variables during execution.
     *                           It allows access to and modification of local variables, including the one being incremented.
     * @param callStack          The virtual machine's call stack, keeping track of the method invocation hierarchy.
     *                           Used by instructions that involve method calls or returns (e.g., `CALL` and `RETURN`).
     * @return The updated program counter-value, which is typically the current PC incremented by 1, unless control flow is altered.
     */
    @Override
    public int execute(String[] parts, int currentPC, OperandStack operandStack, LocalVariableStore localVariableStore, CallStack callStack) {
        // Retrieve the index of the local variable and the increment value from the parameters
        int localVariableIndex = Integer.parseInt(parts[1]); // Index of the local variable to be incremented
        long incrementValue = Long.parseLong(parts[2]); // The value by which to increment the local variable

        // Get the current value of the local variable at the specified index
        long currentValue = (long) callStack.peekFrame().getLocalVariableStore().getVariable(localVariableIndex);

        // Increment the local variable value by the specified increment
        long newValue = currentValue + incrementValue;

        // Update the local variable with the new incremented value
        callStack.peekFrame().getLocalVariableStore().setVariable(localVariableIndex, newValue);

        // Return the updated program counter (next instruction)
        return currentPC + 1;
    }
}
