package org.jcnc.snow.vm.commands.ref.control;

import org.jcnc.snow.vm.interfaces.Command;
import org.jcnc.snow.vm.module.CallStack;
import org.jcnc.snow.vm.module.LocalVariableStore;
import org.jcnc.snow.vm.module.OperandStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The {@code RPushCommand} class implements the {@link Command} interface and provides
 * the "reference push" instruction ({@code R_PUSH}) for the virtual machine.
 * <p>
 * <b>Function:</b> Pushes a reference-type value (String literal or array literal) onto the operand stack.
 * </p>
 *
 * <h2>Supported Literals</h2>
 * <ul>
 *     <li><b>String Literals:</b> Quoted strings (e.g., {@code "hello\nworld"}) with escape sequence support.</li>
 *     <li><b>Array Literals:</b> Bracketed array forms (e.g., {@code [1, 2, [3, 4]]}), including nested arrays.</li>
 * </ul>
 *
 * <h2>Implementation Details</h2>
 * <ul>
 *     <li>Array literals are parsed into <b>mutable</b> {@link java.util.ArrayList} objects, to support in-place modification (e.g., by {@code ARR_SET}).</li>
 *     <li>String literals wrapped in quotes are automatically unescaped according to Java string escape rules.</li>
 *     <li>Handles atomic values: numbers (including hex, binary, float, long, short, byte), booleans, and fallback to string.</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>
 *     R_PUSH "hello\nworld"     // pushes String "hello\nworld" (with actual newline)
 *     R_PUSH [1, 2, 3]         // pushes ArrayList {1, 2, 3}
 *     R_PUSH [1, [2, 3], 4]    // pushes nested arrays as mutable lists
 * </pre>
 */
public class RPushCommand implements Command {

    /**
     * Executes the {@code R_PUSH} instruction. Parses the given literal parameter and pushes it onto the operand stack.
     * <p>
     * Handles:
     * <ul>
     *     <li>Array literals (e.g., {@code [1, 2, "a"]}), parsed recursively as mutable ArrayLists</li>
     *     <li>Quoted string literals (e.g., {@code "abc\n"}), parsed with escape sequence support</li>
     *     <li>Unquoted raw strings, numbers, and atoms</li>
     * </ul>
     *
     * @param parts     The instruction split into parts (opcode and arguments)
     * @param pc        The current program counter
     * @param stack     The operand stack to push the value onto
     * @param local     The local variable store (unused)
     * @param callStack The call stack (unused)
     * @return The next program counter (pc + 1)
     * @throws IllegalStateException if the R_PUSH parameter is missing or parsing fails
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

        // Handle array literal
        if (literal.startsWith("[") && literal.endsWith("]")) {
            Object parsed = parseValue(new Cursor(literal));
            if (!(parsed instanceof List<?> list)) {
                stack.push(parsed);
            } else {
                stack.push(deepMutable(list));
            }
        }
        // String literal with quotes and escapes
        else if (literal.length() >= 2 && literal.startsWith("\"") && literal.endsWith("\"")) {
            String decoded = parseQuoted(new Cursor(literal));
            stack.push(decoded);
        }
        // Raw atom or string
        else {
            stack.push(literal);
        }
        return pc + 1;
    }

    /**
     * Utility class for string parsing, used by the array and string literal parsers.
     */
    static class Cursor {
        final String s;
        int i;

        /**
         * Constructs a cursor over the provided string.
         * @param s the input string to parse
         */
        Cursor(String s) { this.s = s; this.i = 0; }

        /**
         * Advances the cursor by one character.
         */
        void skip() { i++; }

        /**
         * Returns true if the cursor has reached the end of the string.
         * @return true if end of string
         */
        boolean end() { return i >= s.length(); }

        /**
         * Returns the current character at the cursor position.
         * @return the current character
         */
        char ch() { return s.charAt(i); }
    }

    /**
     * Parses a value from the current cursor position.
     * Supports arrays, quoted strings, or atoms.
     *
     * @param c the parsing cursor
     * @return the parsed object (List, String, Number, Boolean, or String fallback)
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
     * Skips whitespace characters at the cursor.
     * @param c the parsing cursor
     */
    private static void skipWs(Cursor c) {
        while (!c.end()) {
            char ch = c.ch();
            if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n') c.skip();
            else break;
        }
    }

    /**
     * Parses an array literal of the form [elem1, elem2, ...] (may be nested).
     * Recursively parses elements using {@link #parseValue(Cursor)}.
     *
     * @param c the parsing cursor
     * @return a List of parsed elements
     */
    private Object parseArray(Cursor c) {
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
     * Parses a quoted string, handling standard Java escape sequences (e.g. \n, \t, uXXXX).
     *
     * @param c the parsing cursor
     * @return the decoded string
     */
    private static String parseQuoted(Cursor c) {
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
                    case 'f' -> sb.append('\f');
                    case 'b' -> sb.append('\b');
                    case '\"' -> sb.append('\"');
                    case '\'' -> sb.append('\'');
                    case '\\' -> sb.append('\\');
                    case 'u' -> { // Unicode escape: uXXXX
                        StringBuilder uni = new StringBuilder();
                        for (int k = 0; k < 4 && !c.end(); ++k) {
                            uni.append(c.ch());
                            c.skip();
                        }
                        try {
                            int code = Integer.parseInt(uni.toString(), 16);
                            sb.append((char) code);
                        } catch (Exception e) {
                            // Invalid unicode, append as is
                            sb.append("\\u").append(uni);
                        }
                    }
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
     * Parses an atomic value (number, boolean, or fallback string) from the cursor.
     *
     * @param c the parsing cursor
     * @return the parsed object (Integer, Double, Float, Long, Boolean, or String)
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
        // Try number parsing with various notations and types
        try {
            if (token.startsWith("0x") || token.startsWith("0X")) {
                return Integer.parseInt(token.substring(2), 16);
            }
            if (token.startsWith("0b") || token.startsWith("0B")) {
                return Integer.parseInt(token.substring(2), 2);
            }
            if (token.endsWith("f")) {
                return Float.parseFloat(token.substring(0, token.length() - 1));
            }
            if (token.endsWith("L")) {
                return Long.parseLong(token.substring(0, token.length() - 1));
            }
            if (token.endsWith("s")) {
                return Short.parseShort(token.substring(0, token.length() - 1));
            }
            if (token.endsWith("b")) {
                return Byte.parseByte(token.substring(0, token.length() - 1));
            }
            if (token.equals("1")) return true;
            if (token.equals("0")) return false;
            if (token.contains(".")) {
                return Double.parseDouble(token);
            }
            return Integer.parseInt(token);
        } catch (NumberFormatException e) {
            // fallback as string
            return token;
        }
    }

    /**
     * Creates a deeply unmodifiable version of the provided list (and its nested lists).
     *
     * @param l the original list
     * @return an unmodifiable view of the list and all nested lists
     */
    List<?> deepUnmodifiable(List<?> l) {
        List<Object> out = new ArrayList<>(l.size());
        for (Object v : l) out.add(deepUnmodifiableObject(v));
        return Collections.unmodifiableList(out);
    }

    /**
     * Helper for {@link #deepUnmodifiable(List)}; handles nested lists recursively.
     *
     * @param v the object to process
     * @return an unmodifiable list if input is a list; otherwise, the object itself
     */
    Object deepUnmodifiableObject(Object v) {
        if (v instanceof List<?> l) {
            return deepUnmodifiable(l);
        }
        return v;
    }

    /**
     * Creates a deeply mutable version of the provided list (and its nested lists).
     *
     * @param l the original list
     * @return a new mutable list (ArrayList), with all nested lists mutable
     */
    private static java.util.List<?> deepMutable(java.util.List<?> l) {
        java.util.List<Object> out = new java.util.ArrayList<>(l.size());
        for (Object v : l) out.add(deepMutableObject(v));
        return out;
    }

    /**
     * Helper for {@link #deepMutable(List)}; handles nested lists recursively.
     *
     * @param v the object to process
     * @return a mutable list if input is a list; otherwise, the object itself
     */
    private static Object deepMutableObject(Object v) {
        if (v instanceof java.util.List<?> l) {
            return deepMutable(l);
        }
        return v;
    }
}
