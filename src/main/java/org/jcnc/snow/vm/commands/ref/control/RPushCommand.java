package org.jcnc.snow.vm.commands.ref.control;

import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The {@code RPushCommand} class implements the {@link Command} interface
 * and represents the "reference push" instruction ({@code R_PUSH}) in the virtual machine.
 *
 * <p>
 * This instruction pushes a reference-type value onto the operand stack.
 * The input is parsed from the textual instruction form, which can represent:
 * <ul>
 *     <li>String literals</li>
 *     <li>Array literals (e.g., {@code [1, 2, 3]}), including nested arrays</li>
 * </ul>
 * </p>
 *
 * <p>
 * For array literals, a nested list structure is constructed. In this implementation,
 * array literals are pushed as <b>mutable</b> {@link java.util.ArrayList} structures,
 * so that subsequent system calls such as {@code ARR_SET} can modify elements in-place.
 * </p>
 */
public class RPushCommand implements Command {

    /**
     * Executes the R_PUSH command.
     *
     * @param parts     The parts of the instruction, where {@code parts[1..n]} are concatenated as the literal.
     * @param pc        The current program counter.
     * @param stack     The operand stack where the result will be pushed.
     * @param local     The local variable store (unused in this instruction).
     * @param callStack The call stack (unused in this instruction).
     * @return The new program counter (typically {@code pc+1}).
     * @throws IllegalStateException if no literal parameter is provided.
     */
    @Override
    public int execute(String[] parts, int pc, OperandStack stack, LocalVariableStore local, CallStack callStack) {
        if (parts.length < 2)
            throw new IllegalStateException("R_PUSH missing parameter");

        // Join all arguments into a complete literal string
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < parts.length; i++) {
            if (i > 1) sb.append(' ');
            sb.append(parts[i]);
        }
        String literal = sb.toString().trim();

        // Check if this is an array literal
        if (literal.startsWith("[") && literal.endsWith("]")) {
            Object parsed = parseValue(new Cursor(literal));
            if (!(parsed instanceof List<?> list)) {
                // Should not happen in theory; safety fallback
                stack.push(parsed);
            } else {
                // Push a deep-mutable copy so ARR_SET can modify elements in-place
                stack.push(deepMutable(list));
            }
        } else {
            // Regular string, push as-is
            stack.push(literal);
        }
        return pc + 1;
    }

    /**
     * A simple string cursor, supporting index increment and character reading, for use by the parser.
     */
    static class Cursor {
        final String s;
        int i;

        /**
         * Constructs a new {@code Cursor} for the given string.
         *
         * @param s The string to parse.
         */
        Cursor(String s) {
            this.s = s;
            this.i = 0;
        }

        /**
         * Advances the cursor by one character.
         */
        void skip() {
            i++;
        }

        /**
         * @return {@code true} if the cursor has reached the end of the string.
         */
        boolean end() {
            return i >= s.length();
        }

        /**
         * Gets the character at the current cursor position.
         *
         * @return current character
         * @throws StringIndexOutOfBoundsException if at end of string
         */
        char ch() {
            return s.charAt(i);
        }
    }

    /**
     * Parses a value from the input string at the current cursor position.
     * This can be an array literal, a quoted string, or a simple atom (number, word).
     *
     * @param c The cursor for parsing.
     * @return The parsed value (could be List, String, Number).
     */
    Object parseValue(Cursor c) {
        skipWs(c);
        if (c.end()) return "";
        char ch = c.ch();
        if (ch == '[') return parseArray(c);
        if (ch == '\"') return parseQuoted(c);
        return parseAtom(c);
    }

    /**
     * Skips whitespace characters in the input string.
     *
     * @param c The cursor to advance.
     */
    private static void skipWs(Cursor c) {
        while (!c.end()) {
            char ch = c.ch();
            if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n') c.skip();
            else break;
        }
    }

    /**
     * Parses an array literal from the input, including nested arrays.
     *
     * @param c The cursor (positioned at '[' at entry).
     * @return A List representing the parsed array.
     */
    private Object parseArray(Cursor c) {
        // assumes current char is '['
        c.skip(); // skip '['
        List<Object> out = new ArrayList<>();
        skipWs(c);
        while (!c.end()) {
            if (c.ch() == ']') {
                c.skip(); // skip ']'
                break;
            }
            Object v = parseValue(c);
            out.add(v);
            skipWs(c);
            if (!c.end() && c.ch() == ',') {
                c.skip();
                skipWs(c);
            }
        }
        return out;
    }

    /**
     * Parses a quoted string literal, handling escape characters.
     *
     * @param c The cursor (positioned at '"' at entry).
     * @return The parsed string value.
     */
    private static String parseQuoted(Cursor c) {
        // assumes current char is '"'
        c.skip(); // skip opening quote
        StringBuilder sb = new StringBuilder();
        while (!c.end()) {
            char ch = c.ch();
            c.skip();
            if (ch == '\\') {
                if (c.end()) break;
                char esc = c.ch();
                c.skip();
                switch (esc) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case '\"' -> sb.append('\"');
                    case '\\' -> sb.append('\\');
                    default -> sb.append(esc);
                }
            } else if (ch == '\"') {
                break;
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    /**
     * Parses an atom (number, hexadecimal, binary, or plain string token).
     *
     * @param c The cursor.
     * @return An Integer, Double, or String, depending on the content.
     */
    private static Object parseAtom(Cursor c) {
        StringBuilder sb = new StringBuilder();
        while (!c.end()) {
            char ch = c.ch();
            if (ch == ',' || ch == ']' || ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n') break;
            sb.append(ch);
            c.skip();
        }
        String token = sb.toString();
        // try number
        try {
            if (token.startsWith("0x") || token.startsWith("0X")) {
                return Integer.parseInt(token.substring(2), 16);
            }
            if (token.startsWith("0b") || token.startsWith("0B")) {
                return Integer.parseInt(token.substring(2), 2);
            }
            if (token.contains(".")) {
                return Double.parseDouble(token);
            }
            return Integer.parseInt(token);
        } catch (NumberFormatException e) {
            // fallback as string
            return token;
        }
    }

    // ---------------------- helpers for immutability/mutability ----------------------

    /**
     * Recursively creates an unmodifiable copy of a list, with all nested lists also unmodifiable.
     *
     * @param l The list to make unmodifiable.
     * @return An unmodifiable deep copy of the list.
     */
    List<?> deepUnmodifiable(List<?> l) {
        List<Object> out = new ArrayList<>(l.size());
        for (Object v : l) out.add(deepUnmodifiableObject(v));
        return Collections.unmodifiableList(out);
    }

    /**
     * Helper method for {@link #deepUnmodifiable(List)}. Recursively processes each element.
     *
     * @param v The object to process.
     * @return Unmodifiable list if input is a list, otherwise the value itself.
     */
    Object deepUnmodifiableObject(Object v) {
        if (v instanceof List<?> l) {
            return deepUnmodifiable(l);
        }
        return v;
    }

    /**
     * Create a deep mutable copy of a nested List structure, preserving element values.
     * Nested lists are turned into {@link java.util.ArrayList} so they can be modified by ARR_SET.
     *
     * @param l The source list.
     * @return Deep mutable copy of the list.
     */
    private static java.util.List<?> deepMutable(java.util.List<?> l) {
        java.util.List<Object> out = new java.util.ArrayList<>(l.size());
        for (Object v : l) out.add(deepMutableObject(v));
        return out;
    }

    /**
     * Helper method for {@link #deepMutable(List)}. Recursively processes each element.
     *
     * @param v The object to process.
     * @return Mutable list if input is a list, otherwise the value itself.
     */
    private static Object deepMutableObject(Object v) {
        if (v instanceof java.util.List<?> l) {
            return deepMutable(l);
        }
        return v;
    }
}
