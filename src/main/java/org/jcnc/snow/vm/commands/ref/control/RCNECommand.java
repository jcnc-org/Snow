package org.jcnc.snow.vm.commands.ref.control;

import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;
import org.jcnc.snow.vm.utils.LoggingUtils;

import java.util.Objects;

/**
 * {@code RCNECommand} implements the {@link Command} interface and represents the
 * reference inequality comparison instruction ({@code R_CNE}) in the virtual machine.
 *
 * <p>
 * The {@code R_CNE} instruction compares two object references for inequality using
 * {@link java.util.Objects#equals(Object, Object)}. If the comparison evaluates to {@code false}
 * (i.e., the operands are not equal), the instruction performs a conditional branch to the
 * specified target address; otherwise, execution continues sequentially with the next instruction.
 * </p>
 *
 * <p><b>Instruction Format</b></p>
 * <ul>
 *   <li><b>Mnemonic:</b> {@code R_CNE}</li>
 *   <li><b>Operands:</b> One operand (the branch target address)</li>
 * </ul>
 *
 * <p><b>Behavior</b></p>
 * <ol>
 *   <li>Parses the branch target address from {@code parts[1]}.</li>
 *   <li>Pops the right operand (reference object) from the top of the operand stack.</li>
 *   <li>Pops the left operand (reference object) from the operand stack.</li>
 *   <li>Compares the two operands using {@code Objects.equals(left, right)}.</li>
 *   <li>If the comparison result is {@code false}, updates the program counter (PC) to the target branch address.</li>
 *   <li>If the comparison result is {@code true}, increments the PC to the next sequential instruction ({@code currentPC + 1}).</li>
 * </ol>
 *
 * <p><b>Notes</b></p>
 * <ul>
 *   <li>This instruction performs a <i>value-based</i> inequality check rather than a reference identity check.</li>
 *   <li>It safely handles {@code null} operands, as {@code Objects.equals()} returns {@code false} when only one operand is {@code null}.</li>
 *   <li>Primarily used to implement conditional branching constructs (e.g., {@code if}, {@code while}) based on object inequality.</li>
 * </ul>
 *
 * <p><b>Example Usage</b></p>
 * <pre>{@code
 * ; Pseudocode representation
 * push "foo"
 * push "bar"
 * R_CNE target=100
 * ; if not equal, jumps to address 100
 * }</pre>
 *
 * <p>
 * This command provides reference-level inequality branching support within
 * high-level language constructs executed by the Snow Virtual Machine.
 * </p>
 */
public final class RCNECommand implements Command {

    @Override
    public int execute(String[] parts,
                       int currentPC,
                       OperandStack operandStack,
                       LocalVariableStore localVariableStore,
                       CallStack callStack) {
        int target = Integer.parseInt(parts[1]);
        Object right = operandStack.pop();
        Object left = operandStack.pop();

        if (!Objects.equals(left, right)) {
            LoggingUtils.logInfo("Jumping to command", String.valueOf(target));
            return target;
        }

        return currentPC + 1;
    }
}
