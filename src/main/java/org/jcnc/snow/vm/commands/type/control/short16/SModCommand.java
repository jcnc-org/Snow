package org.jcnc.snow.vm.commands.type.control.short16;

import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * SModCommand Opcode: Represents the short16 modulus operation in the virtual machine.
 * <p>This opcode is implemented by the {@link SModCommand} class, which defines its specific execution logic.</p>
 *
 * <p>Execution Steps:</p>
 * <ol>
 *     <li>Pops the top two short16 values from the operand stack.</li>
 *     <li>Performs the modulus operation to calculate the remainder (i.e., <code>a % b</code>).</li>
 *     <li>Pushes the result of the modulus operation back onto the operand stack for subsequent instructions to use.</li>
 * </ol>
 *
 * <p>This opcode is typically used to calculate the remainder of the division of two short16 values, making it a fundamental operation for arithmetic logic within the virtual machine.</p>
 */
public class SModCommand implements Command {

    /**
     * Default constructor for creating an instance of SModCommand.
     * This constructor is empty as no specific initialization is required.
     */
    public SModCommand() {
        // Empty constructor
    }

    /**
     * Executes the virtual machine instruction's operation for performing a modulus operation between two short16 values.
     *
     * <p>This method retrieves the two short16 values from the operand stack, performs the modulus operation, and pushes the result back onto the operand stack.</p>
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
        // Pop the top two operands from the stack
        short b = (short) operandStack.pop();
        short a = (short) operandStack.pop();

        // Perform the modulus operation and push the result back onto the stack
        operandStack.push((short) (a % b));

        // Return the updated program counter (next instruction)
        return currentPC + 1;
    }
}
