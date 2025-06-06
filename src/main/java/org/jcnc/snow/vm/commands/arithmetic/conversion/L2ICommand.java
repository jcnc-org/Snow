package org.jcnc.snow.vm.commands.arithmetic.conversion;

import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * L2ICommand Opcode: Represents the type conversion operation from long64 to int32 in the virtual machine.
 * <p>This opcode is implemented by the {@link L2ICommand} class, which defines its specific execution logic.</p>
 *
 * <p>Execution Steps:</p>
 * <ol>
 *     <li>Pop the top long64 value from the operand stack.</li>
 *     <li>Convert the long64 value to an int32 value (this may involve truncation).</li>
 *     <li>Push the converted int32 value back onto the operand stack for subsequent operations.</li>
 * </ol>
 *
 * <p>This opcode is typically used to narrow a long64 value to an int32 type for further integer operations.</p>
 */
public class L2ICommand implements Command {

    /**
     * Default constructor for creating an instance of L2ICommand.
     */
    public L2ICommand() {
        // Empty constructor
    }

    /**
     * Executes the long64 to int32 conversion operation.
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
        long value = (long) operandStack.pop();
        int convertedValue = (int) value;
        operandStack.push(convertedValue);
        return currentPC + 1;
    }
}
