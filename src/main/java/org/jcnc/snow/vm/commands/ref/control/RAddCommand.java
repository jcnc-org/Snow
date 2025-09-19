package org.jcnc.snow.vm.commands.ref.control;

import org.jcnc.snow.vm.commands.system.control.SyscallUtils;
import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.math.BigDecimal;
import java.math.BigInteger;
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
     * Converts an arbitrary object into a display string that mimics console output behavior.
     * <p>
     * Rules:
     * <ul>
     *   <li><code>null</code> → <code>"null"</code></li>
     *   <li><code>byte[]</code> → Decoded as UTF-8 text</li>
     *   <li>Other arrays → Formatted using {@link SyscallUtils#arrayToString(Object)}</li>
     *   <li>All other objects → Result of {@link Object#toString()}</li>
     * </ul>
     *
     * @param obj the object to convert
     * @return a string representation suitable for display
     */

    private static String toDisplayString(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj.getClass().isArray()) {
            // SyscallUtils handles byte[] specially as UTF-8 and others via Arrays.toString/deepToString
            return SyscallUtils.arrayToString(obj);
        }
        return Objects.toString(obj, "null");
    }

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
            // Numeric addition path
            if (l instanceof Double || r instanceof Double) {
                operandStack.push(l.doubleValue() + r.doubleValue());
            } else if (l instanceof Float || r instanceof Float) {
                operandStack.push(l.floatValue() + r.floatValue());
            } else if (l instanceof Long || r instanceof Long) {
                operandStack.push(l.longValue() + r.longValue());
            } else if (l instanceof BigDecimal || r instanceof BigDecimal) {
                BigDecimal ld = (l instanceof BigDecimal bd) ? bd : BigDecimal.valueOf(l.doubleValue());
                BigDecimal rd = (r instanceof BigDecimal bd) ? bd : BigDecimal.valueOf(r.doubleValue());
                operandStack.push(ld.add(rd));
            } else if (l instanceof BigInteger || r instanceof BigInteger) {
                BigInteger li = (l instanceof BigInteger bi) ? bi : BigInteger.valueOf(l.longValue());
                BigInteger ri = (r instanceof BigInteger bi) ? bi : BigInteger.valueOf(r.longValue());
                operandStack.push(li.add(ri));
            } else {
                // Fallback for byte/short/int
                operandStack.push(l.intValue() + r.intValue());
            }
        } else {
            // String concatenation path with smart array support to avoid "[B@xxxx"
            String result = toDisplayString(left) + toDisplayString(right);
            operandStack.push(result);
        }

        return currentPC + 1;
    }
}
