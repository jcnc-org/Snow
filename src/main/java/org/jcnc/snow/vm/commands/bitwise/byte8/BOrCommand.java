package org.jcnc.snow.vm.commands.bitwise.byte8;

import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * The {@code BOrCommand} class implements the {@link Command} interface and represents the byte8 bitwise OR (`|`) operation command.
 * This class performs a byte8 bitwise OR operation in the virtual machine by popping the top two values from the stack,
 * computing their OR, and then pushing the result back onto the stack. It is a basic operation command within the virtual machine.
 *
 * <p><b>Operation details:</b></p>
 * <ul>
 *     <li>Pops two operands from the virtual machine stack.</li>
 *     <li>Performs the byte8 bitwise OR (`|`) operation.</li>
 *     <li>Pushes the result back onto the virtual machine stack.</li>
 * </ul>
 */
public class BOrCommand implements Command {
    /**
     * Default constructor for creating an instance of {@code BOrCommand}.
     * This constructor is empty as no specific initialization is required.
     */
    public BOrCommand() {
        // Empty constructor
    }

    /**
     * Executes the virtual machine instruction's operation.
     *
     * @param parts              The array of instruction parameters, which usually includes the operator and related arguments.
     * @param currentPC          The current program counter-value, indicating the address of the instruction being executed.
     * @param operandStack       The virtual machine's operand stack manager, responsible for performing stack operations.
     * @param localVariableStore The local variable store, typically used to manage method-local variables.
     * @param callStack          The virtual machine's call stack, which keeps track of method invocations.
     * @return The updated program counter-value, typically {@code currentPC + 1}, unless a control flow instruction is executed.
     * @throws IllegalStateException if there are not enough operands on the stack to perform the operation.
     */
    @Override
    public int execute(String[] parts, int currentPC, OperandStack operandStack, LocalVariableStore localVariableStore, CallStack callStack) {
        // Ensure the stack has at least two operands
        if (operandStack.size() < 2) {
            throw new IllegalStateException("Not enough operands on the stack for BOR operation.");
        }

        // Pop the top two operands from the stack
        final byte b = (byte) operandStack.pop();
        final byte a = (byte) operandStack.pop();

        // Perform the byte8 bitwise OR operation and push the result back onto the stack
        operandStack.push((byte)(a | b));

        return currentPC + 1;
    }
}