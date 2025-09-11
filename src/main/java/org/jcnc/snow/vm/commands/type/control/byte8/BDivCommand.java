package org.jcnc.snow.vm.commands.type.control.byte8;

import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * BDivCommand Opcode: Represents the byte8 division operation in the virtual machine.
 * <p>This opcode is implemented by the {@link BDivCommand} class, which defines its specific execution logic.</p>
 *
 * <p>Execution Steps:</p>
 * <ol>
 *     <li>Pops the top two byte8 values from the operand stack.</li>
 *     <li>Performs the division operation (i.e., <code>a / b</code>).</li>
 *     <li>Checks for division by zero to prevent errors.</li>
 *     <li>Pushes the result of the division back onto the operand stack for subsequent instructions to use.</li>
 * </ol>
 *
 * <p>This opcode is typically used to divide one byte8 value by another, making it a fundamental operation for arithmetic logic within the virtual machine.</p>
 */
public class BDivCommand implements Command {

    /**
     * Default constructor for creating an instance of BDivCommand.
     * This constructor does not perform any specific initialization, as the command's parameters are passed during execution.
     */
    public BDivCommand() {
        // Empty constructor
    }

    /**
     * Executes the virtual machine instruction's operation for dividing two byte8 values.
     *
     * <p>This method retrieves the two byte8 values from the operand stack, checks for division by zero, performs the division, and pushes the result back onto the operand stack.</p>
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
        byte b = (byte) operandStack.pop();
        byte a = (byte) operandStack.pop();

        // Check for division by zero
        if (b == 0) {
            throw new ArithmeticException("Division by zero is not allowed.");
        }

        // Perform the division and push the result back onto the stack
        operandStack.push((byte) (a / b));

        // Return the updated program counter (next instruction)
        return currentPC + 1;
    }
}
