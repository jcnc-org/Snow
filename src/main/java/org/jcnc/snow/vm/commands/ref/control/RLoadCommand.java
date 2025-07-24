package org.jcnc.snow.vm.commands.ref.control;

import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * The {@code RLoadCommand} class implements the {@link Command} interface and represents the
 * reference load instruction ({@code R_LOAD}) in the virtual machine.
 *
 * <p>
 * This instruction loads a reference object from the current stack frame’s local variable store
 * at the specified slot and pushes it onto the operand stack.
 * </p>
 *
 * <p>Instruction format: {@code R_LOAD <slot>}</p>
 * <ul>
 *   <li>{@code <slot>}: The index in the local variable table to load the reference from.</li>
 * </ul>
 *
 * <p>Behavior:</p>
 * <ul>
 *   <li>Parses the slot index from the instruction parameters.</li>
 *   <li>Fetches the reference object stored at the specified slot in the current stack frame's local variable store.</li>
 *   <li>Pushes the fetched reference onto the operand stack.</li>
 *   <li>Increments the program counter to the next instruction.</li>
 * </ul>
 */
public final class RLoadCommand implements Command {

    /**
     * Executes the {@code R_LOAD} instruction, loading a reference from the local variable table and pushing it onto the operand stack.
     *
     * @param parts The instruction parameters. {@code parts[0]} is the operator ("R_LOAD"), {@code parts[1]} is the slot index.
     * @param pc The current program counter value, indicating the instruction address being executed.
     * @param stack The operand stack manager. The loaded reference will be pushed onto this stack.
     * @param lvs The local variable store. (Not used directly, as this command uses the store from the current stack frame.)
     * @param cs The call stack manager. The reference will be loaded from the local variable store of the top stack frame.
     * @return The next program counter value ({@code pc + 1}), pointing to the next instruction.
     * @throws NumberFormatException if the slot parameter cannot be parsed as an integer.
     */
    @Override
    public int execute(String[] parts, int pc,
                       OperandStack stack,
                       LocalVariableStore lvs,
                       CallStack cs) {

        int slot = Integer.parseInt(parts[1]);
        Object v = cs.peekFrame().getLocalVariableStore().getVariable(slot);
        stack.push(v);
        return pc + 1;
    }
}
