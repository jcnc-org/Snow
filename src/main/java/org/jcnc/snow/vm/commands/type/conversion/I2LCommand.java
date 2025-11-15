package org.jcnc.snow.vm.commands.type.conversion;

import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;
import org.jcnc.snow.vm.utils.NumberUtils;

/**
 * I2LCommand Opcode: Represents the type conversion operation from int32 to long64 in the virtual machine.
 * <p>This opcode is implemented by the {@link I2LCommand} class, which defines its specific execution logic.</p>
 *
 * <p>Execution Steps:</p>
 * <ol>
 *     <li>Pop the top int32 value from the operand stack.</li>
 *     <li>Convert the int32 value to a long64 value.</li>
 *     <li>Push the converted long64 value back onto the operand stack for subsequent operations.</li>
 * </ol>
 *
 * <p>This opcode is commonly used to widen an int32 value to a long64 type.</p>
 */
public class I2LCommand implements Command {

    /**
     * Default constructor for creating an instance of I2LCommand.
     */
    public I2LCommand() {
        // Empty constructor
    }

    /**
     * Executes the int32 to long64 conversion operation.
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
        long convertedValue = NumberUtils.popInt(operandStack, "I2L");
        operandStack.push(convertedValue);
        return currentPC + 1;
    }
}
