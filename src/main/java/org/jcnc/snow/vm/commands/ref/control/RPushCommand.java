package org.jcnc.snow.vm.commands.ref.control;

import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

/**
 * The {@code RPushCommand} class implements the {@link Command} interface
 * and represents the "reference push" instruction ({@code R_PUSH}) in the virtual machine.
 *
 * <p>
 * This instruction pushes a reference value—typically a string literal or an array literal—onto the operand stack.
 * </p>
 *
 * <p><b>Instruction format:</b> {@code R_PUSH <literal>}</p>
 * <ul>
 *   <li>{@code <literal>}: The reference value (e.g., string, boolean, integer, floating-point, or array literal)
 *   to be pushed onto the stack. If the literal contains spaces, all arguments after {@code R_PUSH} are joined with spaces.</li>
 * </ul>
 *
 * <p><b>Behavior:</b></p>
 * <ul>
 *   <li>Ensures that at least one parameter is provided after the operator; otherwise, throws {@code IllegalStateException}.</li>
 *   <li>Concatenates all parameters after {@code R_PUSH} into a single string, separated by spaces.</li>
 *   <li>If the resulting string is an array literal (i.e., surrounded by square brackets), splits and parses its elements using {@link #getObject(String)} for primitive type support.</li>
 *   <li>Otherwise, pushes the literal as a string reference onto the operand stack.</li>
 *   <li>Increments the program counter to advance to the next instruction.</li>
 * </ul>
 *
 * <p>
 * Supported element types in array literals include integers, floating-point numbers, booleans,
 * and quoted strings (surrounded by double quotes).
 * </p>
 */
public final class RPushCommand implements Command {

    /**
     * Parses a string element into its corresponding Java object.
     * <ul>
     *   <li>If enclosed in double quotes, treats as a string literal (quotes are removed).</li>
     *   <li>If "true" or "false" (case-insensitive), returns 1 or 0 respectively (as integer representation).</li>
     *   <li>If numeric (integer or floating-point), parses accordingly.</li>
     *   <li>Otherwise, returns the string as-is.</li>
     * </ul>
     *
     * @param e The string to parse.
     * @return The parsed object (String, Integer, Double, or Integer for boolean).
     */
    private static Object getObject(String e) {
        String x = e.trim();
        Object v;
        if (x.startsWith("\"") && x.endsWith("\"") && x.length() >= 2) {
            // String literal (remove surrounding double quotes)
            v = x.substring(1, x.length() - 1);
        } else if ("true".equalsIgnoreCase(x) || "false".equalsIgnoreCase(x)) {
            // Boolean value: true → 1, false → 0
            v = Boolean.parseBoolean(x) ? 1 : 0;
        } else {
            try {
                // Attempt to parse as floating-point or integer number
                if (x.contains(".") || x.contains("e") || x.contains("E")) {
                    v = Double.parseDouble(x);
                } else {
                    v = Integer.parseInt(x);
                }
            } catch (NumberFormatException ex) {
                // Fallback: treat as plain string
                v = x;
            }
        }
        return v;
    }

    /**
     * Executes the {@code R_PUSH} instruction: pushes a reference value onto the operand stack.
     *
     * @param parts The instruction split into its components.
     * @param pc    The current program counter.
     * @param stack The operand stack.
     * @param lvs   The local variable store (unused).
     * @param cs    The call stack (unused).
     * @return The next program counter value.
     * @throws IllegalStateException If no parameter is supplied after {@code R_PUSH}.
     */
    @Override
    public int execute(String[] parts, int pc,
                       OperandStack stack,
                       LocalVariableStore lvs,
                       CallStack cs) {

        if (parts.length < 2)
            throw new IllegalStateException("R_PUSH missing parameter");

        // Join all arguments after R_PUSH into a single string, separated by spaces.
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < parts.length; i++) {
            if (i > 1) sb.append(' ');
            sb.append(parts[i]);
        }
        String literal = sb.toString().trim();

        // If the literal is an array (e.g., [1, 2, "foo"]), parse elements and push as an unmodifiable list.
        if (literal.startsWith("[") && literal.endsWith("]")) {
            String inside = literal.substring(1, literal.length() - 1).trim();
            java.util.List<Object> list = new java.util.ArrayList<>();
            if (!inside.isEmpty()) {
                // Split by comma, support element parsing (numbers, booleans, quoted strings)
                String[] elems = inside.split(",");
                for (String e : elems) {
                    Object v = getObject(e);
                    list.add(v);
                }
            }
            // Push as an unmodifiable list to prevent further modifications.
            stack.push(java.util.Collections.unmodifiableList(list));
        } else {
            // Otherwise, push the string literal as-is.
            stack.push(literal);
        }
        return pc + 1;
    }
}
