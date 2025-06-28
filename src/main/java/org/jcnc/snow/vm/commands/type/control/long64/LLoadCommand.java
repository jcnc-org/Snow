package org.jcnc.snow.vm.commands.type.control.long64;

import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * The LLoadCommand class implements the {@link Command} interface and represents a load instruction in the virtual machine.
 * This class is used to load a value from the local variable store of the current method frame and push it onto the virtual machine stack.
 *
 * <p>Specific behavior:</p>
 * <ul>
 *     <li>Retrieves a value from the specified index in the local variable store of the current frame.</li>
 *     <li>Pushes that value onto the virtual machine operand stack.</li>
 *     <li>Returns the updated program counter-value, indicating the continuation of the next instruction.</li>
 * </ul>
 */
public class LLoadCommand implements Command {
    /**
     * Default constructor for creating an instance of LLoadCommand.
     * This constructor is empty as no specific initialization is required.
     */
    public LLoadCommand() {
        // Empty constructor
    }

    /**
     * Executes the load instruction to retrieve a value from the local variable store and push it onto the operand stack.
     *
     * <p>This method performs the following operations:</p>
     * <ul>
     *     <li>Parses the index of the local variable to load from the instruction parameters.</li>
     *     <li>Retrieves the corresponding value from the local variable store of the current method frame.</li>
     *     <li>Pushes the retrieved value onto the operand stack for subsequent operations.</li>
     *     <li>Increments the program counter to point to the next instruction to be executed.</li>
     * </ul>
     *
     * <p>After execution, the program counter (PC) is updated to continue with the next instruction in the sequence, unless control flow changes.</p>
     *
     * @param parts              The array of instruction parameters, which typically includes the operation type and the index
     *                           of the local variable to be loaded. The structure may vary based on the specific instruction.
     * @param currentPC          The current program counter-value, indicating the address of the instruction being executed.
     *                           This value is typically incremented after execution to point to the next instruction.
     * @param operandStack       The virtual machine's operand stack manager, responsible for pushing, popping, and peeking values.
     * @param localVariableStore The local variable store, from which values are loaded based on the provided index.
     *                           This store manages method-local variables.
     * @param callStack          The virtual machine's call stack, which tracks method calls and returns. It is used to access
     *                           the correct frame's local variable store in this case.
     * @return The updated program counter-value, which is typically incremented by 1 unless control flow is modified by other instructions.
     */
    @Override
    public int execute(String[] parts, int currentPC, OperandStack operandStack, LocalVariableStore localVariableStore, CallStack callStack) {
        // Parse the index of the local variable to be loaded
        int index = Integer.parseInt(parts[1]);

        // Retrieve the value from the local variable store of the current method frame
        long value = (long) callStack.peekFrame().getLocalVariableStore().getVariable(index);

        // Push the loaded value onto the operand stack for subsequent operations
        operandStack.push(value);

        // Return the updated program counter to continue to the next instruction
        return currentPC + 1;
    }
}
