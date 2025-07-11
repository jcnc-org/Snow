package org.jcnc.snow.vm.commands.type.conversion;

import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * D2FCommand Opcode: Represents the type conversion operation from double64 to float32 in the virtual machine.
 * <p>This opcode is implemented by the {@link D2FCommand} class, which defines its specific execution logic.</p>
 *
 * <p>Execution Steps:</p>
 * <ol>
 *     <li>Pop the top double64 value from the operand stack.</li>
 *     <li>Convert the double64 value to a float32 value.</li>
 *     <li>Push the converted float32 value back onto the operand stack for subsequent operations.</li>
 * </ol>
 *
 * <p>This opcode is used to narrow a double64 value to a float32 type.</p>
 */
public class D2FCommand implements Command {

    /**
     * Default constructor for creating an instance of D2FCommand.
     */
    public D2FCommand() {
        // Empty constructor
    }

    /**
     * Executes the double64 to float32 conversion operation.
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
        double value = (double) operandStack.pop();
        float convertedValue = (float) value;
        operandStack.push(convertedValue);
        return currentPC + 1;
    }
}
