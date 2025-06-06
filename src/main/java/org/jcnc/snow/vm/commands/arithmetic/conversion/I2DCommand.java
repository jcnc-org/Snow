package org.jcnc.snow.vm.commands.arithmetic.conversion;

import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * I2DCommand Opcode: Represents the type conversion operation from int32 to double64 in the virtual machine.
 * <p>This opcode is implemented by the {@link I2DCommand} class, which defines its specific execution logic.</p>
 *
 * <p>Execution Steps:</p>
 * <ol>
 *     <li>Pop the top int32 value from the operand stack.</li>
 *     <li>Convert the int32 value to a double64 value.</li>
 *     <li>Push the converted double64 value back onto the operand stack for subsequent operations.</li>
 * </ol>
 *
 * <p>This opcode is used to widen an int32 value to a double64 type, providing high-precision floating-point calculations.</p>
 */
public class I2DCommand implements Command {

    /**
     * Default constructor for creating an instance of I2DCommand.
     */
    public I2DCommand() {
        // Empty constructor
    }

    /**
     * Executes the int32 to double64 conversion operation.
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
        double convertedValue = (int) operandStack.pop();
        operandStack.push(convertedValue);
        return currentPC + 1;
    }
}
