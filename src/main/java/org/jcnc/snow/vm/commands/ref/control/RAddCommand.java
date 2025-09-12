package org.jcnc.snow.vm.commands.ref.control;

import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.util.Objects;

/**
 * {@code RAddCommand} implements the {@link Command} interface and represents the
 * reference addition (concatenation) instruction ({@code R_ADD}) in the virtual machine.
 *
 * <p>
 * The {@code R_ADD} instruction performs either:
 * <ul>
 *   <li><b>Numeric addition:</b> If both operands are numeric types, it computes their sum using the widest type necessary to prevent precision loss.</li>
 *   <li><b>String concatenation:</b> For all other cases, it concatenates the string representations of the operands in left-to-right order.</li>
 * </ul>
 * The result is then pushed back onto the operand stack.
 * </p>
 *
 * <p>Instruction Format</p>
 * <ul>
 *   <li><b>Mnemonic:</b> {@code R_ADD}</li>
 *   <li><b>Operands:</b> None (operands are implicitly taken from the top of the operand stack)</li>
 * </ul>
 *
 * <p>Behavior</p>
 * <ol>
 *   <li>Pops the right operand from the operand stack.</li>
 *   <li>Pops the left operand from the operand stack.</li>
 *   <li>If both operands are instances of {@link Number}:
 *     <ul>
 *       <li>Adds them using the most precise applicable numeric type (Double &gt; Float &gt; Long &gt; Int).</li>
 *     </ul>
 *   </li>
 *   <li>Otherwise, converts both operands to their string representations (treating {@code null} as the string {@code "null"}) and concatenates them ({@code left + right}).</li>
 *   <li>Pushes the result onto the operand stack.</li>
 *   <li>Returns the next instruction address ({@code currentPC + 1}).</li>
 * </ol>
 *
 * <p>
 * This command is commonly used to support string concatenation and flexible value addition in scripts or programs running on the virtual machine.
 * </p>
 */
public final class RAddCommand implements Command {
    /**
     * Executes the {@code R_ADD} instruction, performing numeric addition or string concatenation.
     *
     * <p>
     * Pops two operands from the operand stack, computes their sum if both are numbers,
     * or concatenates their string representations otherwise. The result is then pushed back onto the operand stack.
     * </p>
     *
     * @param parts              The array of instruction parameters. Not used by {@code R_ADD}.
     * @param currentPC          The current program counter, indicating the address of the instruction being executed.
     * @param operandStack       The operand stack manager, providing access to the operand stack.
     * @param localVariableStore The local variable store for method-local variables. Not used in this command.
     * @param callStack          The call stack for method invocation hierarchies. Not used in this command.
     * @return The address of the next instruction to execute ({@code currentPC + 1}).
     */
    @Override
    public int execute(String[] parts,
                       int currentPC,
                       OperandStack operandStack,
                       LocalVariableStore localVariableStore,
                       CallStack callStack) {

        Object right = operandStack.pop();
        Object left = operandStack.pop();

        // Fast path: If both operands are numbers, perform numeric addition with widest type
        if (left instanceof Number l && right instanceof Number r) {
            if (left instanceof Double || right instanceof Double) {
                operandStack.push(l.doubleValue() + r.doubleValue());
            } else if (left instanceof Float || right instanceof Float) {
                operandStack.push(l.floatValue() + r.floatValue());
            } else if (left instanceof Long || right instanceof Long) {
                operandStack.push(l.longValue() + r.longValue());
            } else {
                // Fallback for byte/short/int
                operandStack.push(l.intValue() + r.intValue());
            }
        } else {
            // Default path: concatenate string representations (null-safe)
            String result = Objects.toString(left, "null") + Objects.toString(right, "null");
            operandStack.push(result);
        }

        return currentPC + 1;
    }
}
