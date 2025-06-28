package org.jcnc.snow.vm.commands.register.control;

import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * The MovCommand class implements the {@link Command} interface and represents a MOV instruction in the virtual machine.
 * This class is used to transfer a value from one memory location to another within the local variable store of the current frame.
 *
 * <p>Specific behavior:</p>
 * <ul>
 *     <li>Retrieves a value from the specified source index in the local variable store of the current method frame.</li>
 *     <li>Stores the retrieved value into the specified destination index within the same local variable store.</li>
 *     <li>Returns the updated program counter-value, indicating the continuation of the next instruction.</li>
 * </ul>
 */
public class MovCommand implements Command {
    /**
     * Default constructor for creating an instance of MovCommand.
     * This constructor is empty as no specific initialization is required.
     */
    public MovCommand() {
        // Empty constructor
    }

    /**
     * Executes the MOV instruction to transfer a value from one local variable to another within the same method frame.
     *
     * <p>This method performs the following operations:</p>
     * <ul>
     *     <li>Parses the source and destination indices from the instruction parameters.</li>
     *     <li>Retrieves the value from the local variable store at the source index.</li>
     *     <li>Stores the retrieved value into the destination index of the local variable store.</li>
     *     <li>Increments the program counter to point to the next instruction in the program sequence.</li>
     * </ul>
     *
     * <p>After execution, the program counter (PC) is updated to continue with the next instruction, unless control flow is modified.</p>
     *
     * @param parts              The array of instruction parameters, which includes the source and destination indices
     *                           of the local variables involved in the move operation.
     * @param currentPC          The current program counter-value, indicating the address of the instruction being executed.
     *                           Typically, this is incremented after each instruction execution.
     * @param operandStack       The virtual machine's operand stack manager, responsible for pushing, popping, and peeking values.
     * @param localVariableStore The local variable store, used to retrieve and store values between different local variables.
     *                           This store is accessed by the current method frame.
     * @param callStack          The virtual machine's call stack, used to track method calls and manage method frames. It is needed to
     *                           retrieve the correct method frame's local variable store in this command.
     * @return The updated program counter-value, typically incremented by 1, unless modified by control flow instructions.
     */
    @Override
    public int execute(String[] parts, int currentPC, OperandStack operandStack, LocalVariableStore localVariableStore, CallStack callStack) {
        // Parse the source and destination indices from the instruction parameters
        int sourceIndex = Integer.parseInt(parts[1]);
        int destinationIndex = Integer.parseInt(parts[2]);

        // Retrieve the value from the local variable store at the source index
        Object value = callStack.peekFrame().getLocalVariableStore().getVariable(sourceIndex);

        // Store the retrieved value into the destination index within the local variable store
        callStack.peekFrame().getLocalVariableStore().setVariable(destinationIndex, value);

        // Return the updated program counter to continue to the next instruction
        return currentPC + 1;
    }
}
