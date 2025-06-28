package org.jcnc.snow.vm.commands.type.conversion;

import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * S2BCommand Opcode: Represents the type conversion operation from short16 to byte8 in the virtual machine.
 * <p>This opcode is implemented by the {@link S2BCommand} class, which defines its specific execution logic.</p>
 *
 * <p>Execution Steps:</p>
 * <ol>
 *     <li>Pop the top short16 value from the operand stack.</li>
 *     <li>Convert the short16 value to a byte8 value.</li>
 *     <li>Push the converted byte8 value back onto the operand stack for subsequent operations.</li>
 * </ol>
 *
 * <p>This opcode is used to widen a short16 value to a byte8 type, facilitating subsequent integer arithmetic or comparison operations.</p>
 */
public class S2BCommand implements Command {

    /**
     * Default constructor for creating an instance of S2BCommand.
     */
    public S2BCommand() {
        // Empty constructor
    }

    /**
     * Executes the short16 to byte8 conversion operation.
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
        short value = (short) operandStack.pop();
        byte convertedValue = (byte) value;
        operandStack.push(convertedValue);
        return currentPC + 1;
    }
}
