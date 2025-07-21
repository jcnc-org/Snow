package org.jcnc.snow.vm.commands.ref.control;

import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * The {@code RPushCommand} class implements the {@link Command} interface and represents the
 * reference push instruction ({@code R_PUSH}) in the virtual machine.
 *
 * <p>
 * This instruction pushes a reference object, such as a String literal, onto the operand stack.
 * </p>
 *
 * <p>Instruction format: {@code R_PUSH <literal>}</p>
 * <ul>
 *   <li>{@code <literal>}: The reference value (e.g., string) to be pushed onto the stack.
 *   If the literal contains spaces, all parts after {@code R_PUSH} are joined into a single string.</li>
 * </ul>
 *
 * <p>Behavior:</p>
 * <ul>
 *   <li>Checks that the instruction has at least one parameter after the operator.</li>
 *   <li>Concatenates all parameters after {@code R_PUSH} into a single string (separated by spaces).</li>
 *   <li>Pushes the resulting string as a reference object onto the operand stack.</li>
 *   <li>Increments the program counter to the next instruction.</li>
 *   <li>Throws an {@code IllegalStateException} if the instruction is missing required parameters.</li>
 * </ul>
 */
public final class RPushCommand implements Command {

    /**
     * Executes the {@code R_PUSH} instruction, pushing a reference (such as a string literal)
     * onto the operand stack.
     *
     * @param parts The instruction parameters. {@code parts[0]} is the operator ("R_PUSH"),
     *              {@code parts[1..]} are the parts of the literal to be concatenated and pushed.
     * @param pc    The current program counter value, indicating the instruction address being executed.
     * @param stack The operand stack manager. The literal will be pushed onto this stack.
     * @param lvs   The local variable store. (Not used in this instruction.)
     * @param cs    The call stack manager. (Not used in this instruction.)
     * @return The next program counter value ({@code pc + 1}), pointing to the next instruction.
     * @throws IllegalStateException if the instruction is missing required parameters.
     */
    @Override
    public int execute(String[] parts, int pc,
                       OperandStack stack,
                       LocalVariableStore lvs,
                       CallStack cs) {

        if (parts.length < 2)
            throw new IllegalStateException("R_PUSH missing parameter");

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < parts.length; i++) {
            if (i > 1) sb.append(' ');
            sb.append(parts[i]);
        }
        stack.push(sb.toString());
        return pc + 1;
    }
}
