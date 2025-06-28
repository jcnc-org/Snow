package org.jcnc.snow.vm.commands.type.control.double64;

import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * The DStoreCommand class implements the {@link Command} interface and represents a store instruction in the virtual machine.
 * This class is used to store the value from the top of the virtual machine stack into the local variable store, typically used to save computed results into local variables.
 *
 * <p>Specific behavior:</p>
 * <ul>
 *     <li>Pops a value from the virtual machine operand stack.</li>
 *     <li>Stores that value into the specified index location in the local variable store of the current frame on the call stack.</li>
 *     <li>Returns the updated program counter-value, indicating the continuation of the next instruction.</li>
 * </ul>
 */
public class DStoreCommand implements Command {
    /**
     * Default constructor for creating an instance of DStoreCommand.
     * This constructor is empty as no specific initialization is required.
     */
    public DStoreCommand() {
        // Empty constructor
    }

    /**
     * Executes the store instruction.
     *
     * <p>This method retrieves the value from the operand stack, pops the value, and stores it in the local variable store
     * of the current method frame, based on the specified index. The program counter (PC) is then updated to point to the next instruction.</p>
     *
     * <p>The method performs the following actions:</p>
     * <ul>
     *     <li>Parses the index of the local variable where the value will be stored.</li>
     *     <li>Pops the top value from the operand stack.</li>
     *     <li>Stores the popped value into the specified local variable index in the current method frame's local variable store.</li>
     *     <li>Returns the next program counter-value to continue execution of the following instruction.</li>
     * </ul>
     *
     * @param parts              The array of instruction parameters, typically consisting of the operation type and related arguments,
     *                           such as the index for the local variable.
     * @param currentPC          The current program counter-value, indicating the address of the instruction being executed.
     *                           This value is incremented after the execution of each instruction to point to the next one.
     * @param operandStack       The virtual machine's operand stack, used to store and manipulate values during instruction execution.
     * @param localVariableStore The local variable store, used for managing method-local variables.
     * @param callStack          The virtual machine's call stack, which tracks the method invocation hierarchy.
     *                           Used in conjunction with local variable store for method-local variable management.
     * @return The updated program counter-value, typically the current PC value incremented by 1, unless modified by control flow instructions.
     */
    @Override
    public int execute(String[] parts, int currentPC, OperandStack operandStack, LocalVariableStore localVariableStore, CallStack callStack) {
        // Parse the index for the local variable where the value will be stored
        int index = Integer.parseInt(parts[1]);

        // Pop the value from the operand stack
        double value = (double) operandStack.pop();

        // Store the value into the local variable store of the current method frame
        callStack.peekFrame().getLocalVariableStore().setVariable(index, value);

        // Return the updated program counter, which moves to the next instruction
        return currentPC + 1;
    }
}
