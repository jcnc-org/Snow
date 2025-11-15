package org.jcnc.snow.vm.commands.type.control.int32;

import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;
import org.jcnc.snow.vm.utils.NumberUtils;

/**
 * INegCommand Opcode: Represents the int32 negation operation in the virtual machine.
 * <p>This opcode is implemented by the {@link INegCommand} class, which defines its specific execution logic.</p>
 *
 * <p>Execution Steps:</p>
 * <ol>
 *     <li>Pops the top int32 value from the operand stack.</li>
 *     <li>Performs the negation of the popped value (i.e., <code>-value</code>).</li>
 *     <li>Pushes the negated result back onto the operand stack for subsequent instructions to use.</li>
 * </ol>
 *
 * <p>This opcode is typically used to negate an int32 value, making it a fundamental operation for arithmetic logic within the virtual machine.</p>
 */
public class INegCommand implements Command {

    /**
     * Default constructor for creating an instance of INegCommand.
     * This constructor does not perform any specific initialization, as the command's parameters are passed during execution.
     */
    public INegCommand() {
        // Empty constructor
    }

    /**
     * Executes the virtual machine instruction's operation for negating an int32 value.
     *
     * <p>This method retrieves the int32 value from the operand stack, negates it, and pushes the result back onto the operand stack.</p>
     *
     * @param parts              The array of instruction parameters, which is not used in this case since no additional arguments are needed for this operation.
     * @param currentPC          The current program counter-value, indicating the address of the instruction being executed.
     *                           Typically incremented after execution to point to the next instruction.
     * @param operandStack       The operand stack manager of the virtual machine, responsible for operations such as push, pop, and peek.
     * @param localVariableStore The local variable store, used to manage method-local variables during execution.
     *                           It allows access to and modification of local variables, but is not used in this particular operation.
     * @param callStack          The virtual machine's call stack, keeping track of the method invocation hierarchy.
     *                           Used by instructions that involve method calls or returns (e.g., `CALL` and `RETURN`).
     * @return The updated program counter-value, which is typically the current PC incremented by 1, unless control flow is altered.
     */
    @Override
    public int execute(String[] parts, int currentPC, OperandStack operandStack, LocalVariableStore localVariableStore, CallStack callStack) {
        // Pop the top int32 value from the operand stack
        int value = NumberUtils.popInt(operandStack, "I_NEG");

        // Perform the negation of the value
        int negatedValue = -value;

        // Push the negated result back onto the operand stack
        operandStack.push(negatedValue);

        // Return the updated program counter (next instruction)
        return currentPC + 1;
    }
}
