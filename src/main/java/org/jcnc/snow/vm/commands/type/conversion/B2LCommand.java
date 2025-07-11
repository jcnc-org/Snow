package org.jcnc.snow.vm.commands.type.conversion;

import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * B2LCommand Opcode: Represents the type conversion operation from byte8 to long64 in the virtual machine.
 * <p>This opcode is implemented by the {@link B2LCommand} class, which defines its specific execution logic.</p>
 *
 * <p>Execution Steps:</p>
 * <ol>
 *     <li>Pop the top byte8 value from the operand stack.</li>
 *     <li>Convert the byte8 value to a long64 value.</li>
 *     <li>Push the converted long64 value back onto the operand stack for subsequent operations.</li>
 * </ol>
 *
 * <p>This opcode is used to widen a byte8 value to a long64 type.</p>
 */
public class B2LCommand implements Command {

    /**
     * Default constructor for creating an instance of B2LCommand.
     */
    public B2LCommand() {
        // Empty constructor
    }

    /**
     * Executes the byte8 to long64 conversion operation.
     *
     * @param parts              The array of instruction parameters, which is not used in this operation.
     * @param currentPC          The current program counter, representing the instruction address.
     * @param operandStack       The operand stack of the virtual machine.
     * @param localVariableStore The local variable store for managing method-local variables.
     * @param callStack          The call stack of the virtual machine.
     * @return The updated program counter after execution.
     */
    @Override
    public int execute(String[] parts, int currentPC, OperandStack operandStack,
                       LocalVariableStore localVariableStore, CallStack callStack) {
        long convertedValue = (byte) operandStack.pop();
        operandStack.push(convertedValue);
        return currentPC + 1;
    }
}
