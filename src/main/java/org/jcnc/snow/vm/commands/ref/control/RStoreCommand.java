package org.jcnc.snow.vm.commands.ref.control;

import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * The {@code RStoreCommand} class implements the {@link Command} interface and represents the
 * reference store instruction ({@code R_STORE}) in the virtual machine.
 *
 * <p>
 * This instruction pops a reference object from the top of the operand stack
 * and stores it in the local variable table at the specified slot index of the current stack frame.
 * </p>
 *
 * <p>Instruction format: {@code R_STORE <slot>}</p>
 * <ul>
 *   <li>{@code <slot>}: The index in the local variable table where the reference will be stored.</li>
 * </ul>
 *
 * <p>Behavior:</p>
 * <ul>
 *   <li>Parses the slot index from the instruction parameters.</li>
 *   <li>Pops a reference object from the operand stack.</li>
 *   <li>Stores the popped reference object into the local variable table of the current stack frame at the specified slot.</li>
 *   <li>Increments the program counter to the next instruction.</li>
 *   <li>If the operand stack is empty, a {@code java.util.EmptyStackException} may be thrown.</li>
 * </ul>
 */
public final class RStoreCommand implements Command {

    /**
     * Executes the {@code R_STORE} instruction, storing a reference object from the top of the operand stack
     * into the local variable table of the current stack frame.
     *
     * @param parts The instruction parameters. {@code parts[0]} is the operator ("R_STORE"),
     *              {@code parts[1]} is the slot index.
     * @param pc    The current program counter value, indicating the instruction address being executed.
     * @param stack The operand stack manager. The reference object will be popped from this stack.
     * @param lvs   The local variable store. (Not used directly, as the store from the current stack frame is used.)
     * @param cs    The call stack manager. The reference will be stored in the local variable store of the top stack frame.
     * @return The next program counter value ({@code pc + 1}), pointing to the next instruction.
     * @throws NumberFormatException         if the slot parameter cannot be parsed as an integer.
     * @throws java.util.EmptyStackException if the operand stack is empty when popping.
     */
    @Override
    public int execute(String[] parts, int pc,
                       OperandStack stack,
                       LocalVariableStore lvs,
                       CallStack cs) {

        int slot = Integer.parseInt(parts[1]);
        Object v = stack.pop();
        cs.peekFrame().getLocalVariableStore().setVariable(slot, v);
        return pc + 1;
    }
}
