package org.jcnc.snow.vm.commands.type.conversion;

import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * S2DCommand Opcode: Represents the type conversion operation from short16 to double64 in the virtual machine.
 * <p>This opcode is implemented by the {@link S2DCommand} class, which defines its specific execution logic.</p>
 *
 * <p>Execution Steps:</p>
 * <ol>
 *     <li>Pop the top short16 value from the operand stack.</li>
 *     <li>Convert the short16 value to an double64 value.</li>
 *     <li>Push the converted double64 value back onto the operand stack for subsequent operations.</li>
 * </ol>
 *
 * <p>This opcode is used to widen a short16 value to an double64 type, facilitating subsequent integer arithmetic or comparison operations.</p>
 */
public class S2DCommand implements Command {

    /**
     * Default constructor for creating an instance of S2DCommand.
     */
    public S2DCommand() {
        // Empty constructor
    }

    /**
     * Executes the short16 to double64 conversion operation.
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
        double convertedValue = (short) operandStack.pop();
        operandStack.push(convertedValue);
        return currentPC + 1;
    }
}
