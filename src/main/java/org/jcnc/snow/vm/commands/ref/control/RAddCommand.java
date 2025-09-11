package org.jcnc.snow.vm.commands.ref.control;

import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.util.Objects;

/**
 * The {@code RAddCommand} class implements the {@link Command} interface and represents the
 * reference addition (concatenation) instruction ({@code R_ADD}) in the virtual machine.
 *
 * <p>
 * This instruction pops two reference objects from the operand stack, converts them to their string
 * representations, concatenates them in left-to-right order, and pushes the resulting string back onto
 * the operand stack.
 * </p>
 *
 * <p>Instruction format: {@code R_ADD}</p>
 * <ul>
 *   <li>No parameters required. Operands are implicitly taken from the top of the operand stack.</li>
 * </ul>
 *
 * <p>Behavior:</p>
 * <ul>
 *   <li>Pops the right operand (reference object) from the operand stack.</li>
 *   <li>Pops the left operand (reference object) from the operand stack.</li>
 *   <li>Converts both operands to their string representations using {@code String.valueOf()}.</li>
 *   <li>Concatenates the left and right strings ({@code left + right}).</li>
 *   <li>Pushes the concatenated result onto the operand stack.</li>
 *   <li>Increments the program counter to the next instruction.</li>
 * </ul>
 *
 * <p>
 * This command is typically used to concatenate the string representations of two reference objects
 * during script or program execution within the virtual machine.
 * </p>
 */
public final class RAddCommand implements Command {
    /**
     * Executes the virtual machine instruction's operation.
     *
     * <p>
     * This method pops two reference objects from the operand stack, converts them to strings,
     * concatenates them, and pushes the result back onto the operand stack. The program counter is then
     * incremented to point to the next instruction.
     * </p>
     *
     * @param parts              The array of instruction parameters. Not used for {@code R_ADD}.
     * @param currentPC          The current program counter value, indicating the address of the instruction being executed.
     * @param operandStack       The operand stack manager, responsible for managing the stack of operands.
     * @param localVariableStore The local variable store, typically used to manage method-local variables. Not used in this command.
     * @param callStack          The call stack managing method invocation hierarchies. Not used in this command.
     * @return The updated program counter value, typically {@code currentPC + 1}.
     */
    @Override
    public int execute(String[] parts,
                       int currentPC,
                       OperandStack operandStack,
                       LocalVariableStore localVariableStore,
                       CallStack callStack) {

        Object rightOperand = operandStack.pop();   // Right operand
        Object leftOperand = operandStack.pop();    // Left operand

        String result = Objects.toString(leftOperand, "null") + Objects.toString(rightOperand, "null");

        operandStack.push(result);

        return currentPC + 1;
    }
}

