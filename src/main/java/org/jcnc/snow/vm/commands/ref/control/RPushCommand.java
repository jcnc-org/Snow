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
 * This instruction pushes a reference value—typically a string literal or an array literal—onto the operand stack.
 * </p>
 *
 * <p><b>Instruction format:</b> {@code R_PUSH <literal>}</p>
 *
 * <p>
 * Example usages:
 * <ul>
 *   <li>{@code R_PUSH "hello"} &rarr; push the string {@code "hello"} onto stack</li>
 *   <li>{@code R_PUSH [1,2,3]} &rarr; push an (unmodifiable) {@code List<Integer>} onto stack</li>
 *   <li>{@code R_PUSH [[1,2,3],[4,5,6]]} &rarr; push a nested (unmodifiable) {@code List<List<Integer>>} onto stack</li>
 * </ul>
 * </p>
 *
 * <p>
 * Supported element types in array literals include integers, floating-point numbers (parsed as {@code Double}),
 * booleans ({@code true}/{@code false} → {@code 1}/{@code 0}), quoted strings (surrounded by double quotes),
 * and further nested arrays.
 * </p>
 */
public final class RPushCommand implements Command {

    // ======== Parsing helpers ========

    /**
     * Deeply wraps lists as unmodifiable; leaves scalars unchanged.
     *
     * @param v input object
     * @return deeply unmodifiable version of the object
     */
    private static Object deepUnmodifiableObject(Object v) {
        if (v instanceof List<?> l) {
            return deepUnmodifiable(l);
        }
        return v;
    }

    /**
     * Recursively wraps all nested lists as unmodifiable.
     *
     * @param l input list
     * @return deeply unmodifiable list
     */
    private static List<?> deepUnmodifiable(List<?> l) {
        List<Object> out = new ArrayList<>(l.size());
        for (Object v : l) out.add(deepUnmodifiableObject(v));
        return Collections.unmodifiableList(out);
    }

    /**
     * Parses a value starting from the cursor.
     * Skips whitespace, and delegates to the appropriate sub-parser depending on the character:
     * array, quoted string, or atomic value.
     *
     * @param c cursor
     * @return parsed value (Object)
     */
    private static Object parseValue(Cursor c) {
        skipWs(c);
        if (c.end()) return "";
        char ch = c.ch();
        if (ch == '[') return parseArray(c);
        if (ch == '\"') return parseQuoted(c);
        return parseAtom(c);
    }

    /**
     * Parses an array literal from the cursor, supporting nested structures.
     * Assumes the current character is '['.
     *
     * @param c cursor
     * @return List of parsed objects
     */
    private static List<Object> parseArray(Cursor c) {
        // assumes current char is '['
        expect(c, '[');
        skipWs(c);
        List<Object> values = new ArrayList<>();
        if (!peek(c, ']')) {
            while (true) {
                Object v = parseValue(c);
                values.add(v);
                skipWs(c);
                if (peek(c, ',')) {
                    c.i++; // consume ','
                    skipWs(c);
                    continue;
                }
                break;
            }
        }
        expect(c, ']');
        return values;
    }

    // ======== Recursive-descent parser for array literals ========

    /**
     * Parses a string literal wrapped in double quotes (supports common escape sequences).
     * <p>
     * Assumes the cursor currently points to the starting quote character ("), and consumes the opening quote.
     * Parses the string content from the cursor, stopping at the closing quote (").
     * Supported escape sequences:
     * <ul>
     *   <li>\n newline</li>
     *   <li>\r carriage return</li>
     *   <li>\t tab</li>
     *   <li>\" double quote itself</li>
     *   <li>\\ backslash</li>
     *   <li>Other characters are output as-is</li>
     * </ul>
     * If the string is not closed properly (i.e., no closing quote is found before the end), returns the currently parsed content.
     *
     * @param c cursor object (must support ch() for current char, i for index, end() for boundary check)
     * @return parsed string content
     */
    private static String parseQuoted(Cursor c) {
        // Assume current position is the opening quote; consume it
        expect(c, '\"');
        StringBuilder sb = new StringBuilder();

        // Traverse until the end or an unclosed string
        while (!c.end()) {
            char ch = c.ch();
            c.i++;
            if (ch == '\\') { // handle escape sequences
                if (c.end()) break; // nothing after escape char
                char nxt = c.ch();
                c.i++;
                // Common escapes
                switch (nxt) {
                    case 'n' -> sb.append('\n');    // newline
                    case 'r' -> sb.append('\r');    // carriage return
                    case 't' -> sb.append('\t');    // tab
                    case '\"' -> sb.append('\"');   // double quote
                    case '\\' -> sb.append('\\');   // backslash
                    default -> sb.append(nxt);      // any other char as-is
                }
            } else if (ch == '\"') {
                // Found closing quote; end of string
                return sb.toString();
            } else {
                // Regular character
                sb.append(ch);
            }
        }
        // Unclosed string, return parsed content
        return sb.toString();
    }

    /**
     * Parses an atomic constant ("atom"), supporting type-suffixed numbers and booleans.
     * <p>
     * Examples: 0.1f, 123L, 3.14d, 100, true, false<br>
     * Parsing rules:
     * <ul>
     *   <li>Supports float(f/F), long(l/L), double(d/D), short(s/S), byte(b/B) type suffixes</li>
     *   <li>Supports boolean true/false (case-insensitive, converted to 1/0)</li>
     *   <li>Decimals without suffix parsed as double; integers without suffix as int</li>
     *   <li>If parsing fails, returns the original string</li>
     * </ul>
     *
     * @param c cursor, must support ch() for current char, i for index, end() for boundary check, s for the original string
     * @return parsed Object
     */
    private static Object parseAtom(Cursor c) {
        int start = c.i;
        // Read until a comma, ']' or whitespace
        while (!c.end()) {
            char ch = c.ch();
            if (ch == ',' || ch == ']') break;
            if (Character.isWhitespace(ch)) break;
            c.i++;
        }
        // Extract current token
        String token = c.s.substring(start, c.i).trim();
        if (token.isEmpty()) return "";
        // Boolean parsing (case-insensitive, convert to 1/0)
        if ("true".equalsIgnoreCase(token)) return 1;
        if ("false".equalsIgnoreCase(token)) return 0;
        // Handle numeric type suffixes
        try {
            char last = token.charAt(token.length() - 1);
            switch (last) {
                case 'f':
                case 'F':
                    // float suffix
                    return Float.parseFloat(token.substring(0, token.length() - 1));
                case 'l':
                case 'L':
                    // long suffix
                    return Long.parseLong(token.substring(0, token.length() - 1));
                case 'd':
                case 'D':
                    // double suffix
                    return Double.parseDouble(token.substring(0, token.length() - 1));
                case 's':
                case 'S':
                    // short suffix
                    return Short.parseShort(token.substring(0, token.length() - 1));
                case 'b':
                case 'B':
                    // byte suffix
                    return Byte.parseByte(token.substring(0, token.length() - 1));
                default:
                    // No suffix, check for floating point or integer
                    if (token.contains(".") || token.contains("e") || token.contains("E")) {
                        return Double.parseDouble(token);
                    } else {
                        return Integer.parseInt(token);
                    }
            }
        } catch (NumberFormatException ex) {
            // Parsing failed, fall back to original string (e.g. identifiers)
            return token;
        }
    }


    /**
     * Skips all whitespace characters at the current cursor position until a non-whitespace or end of text is reached.
     * <p>
     * The cursor index is automatically incremented, so it will point to the next non-whitespace character (or end of text).
     *
     * @param c cursor object (must support ch() for current char, i for index, end() for boundary check)
     */
    private static void skipWs(Cursor c) {
        // Increment cursor while not at end and is whitespace
        while (!c.end() && Character.isWhitespace(c.ch())) {
            c.i++;
        }
    }

    /**
     * Checks if the current cursor position matches the specified character.
     *
     * @param c  cursor object
     * @param ch expected character
     * @return true if not at end and character matches ch, otherwise false
     */
    private static boolean peek(Cursor c, char ch) {
        return !c.end() && c.ch() == ch;
    }

    /**
     * Asserts that the current cursor position is the specified character; throws if not.
     * If it matches, skips the character and any following whitespace.
     *
     * @param c  cursor object
     * @param ch expected character
     * @throws IllegalArgumentException if current position is not the expected character
     */
    private static void expect(Cursor c, char ch) {
        if (c.end() || c.ch() != ch)
            throw new IllegalArgumentException("R_PUSH array literal parse error: expected '" + ch + "' at position " + c.i);
        c.i++; // Consume current character
        skipWs(c); // Skip any subsequent whitespace
    }


    /**
     * Executes the R_PUSH instruction: pushes a constant or array constant onto the operand stack.
     * <p>
     * Processing steps:
     * <ul>
     *     <li>1. Checks parameter count, throws if insufficient.</li>
     *     <li>2. Concatenates all arguments (except opcode) into a raw literal string.</li>
     *     <li>3. Checks if the literal is an array (starts with [ and ends with ]).</li>
     *     <li>4. If array, recursively parses and pushes as a read-only List onto the operand stack.</li>
     *     <li>5. Otherwise, pushes the literal string as-is.</li>
     * </ul>
     *
     * @param parts instruction and parameter strings (parts[0] is the opcode, others are params)
     * @param pc    current instruction index
     * @param stack operand stack
     * @param lvs   local variable store
     * @param cs    call stack
     * @return next instruction index
     */
    @Override
    public int execute(String[] parts, int pc,
                       OperandStack stack,
                       LocalVariableStore lvs,
                       CallStack cs) {

        // Check parameter count
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
                // Convert to read-only List before pushing to prevent modification
                stack.push(deepUnmodifiable(list));
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
    private static final class Cursor {
        final String s; // Original string
        int i;          // Current index

        Cursor(String s) {
            this.s = s;
            this.i = 0;
        }

        /**
         * Checks if the cursor is at the end of the string.
         *
         * @return true if at end
         */
        boolean end() {
            return i >= s.length();
        }

        /**
         * Gets the character at the current cursor position.
         *
         * @return current character
         */
        char ch() {
            return s.charAt(i);
        }
    }

}
