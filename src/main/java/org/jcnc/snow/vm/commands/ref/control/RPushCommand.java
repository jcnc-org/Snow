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

    private static final class Cursor {
        final String s;
        int i;
        Cursor(String s) { this.s = s; this.i = 0; }
        boolean end() { return i >= s.length(); }
        char ch() { return s.charAt(i); }
    }

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
            Object parsed = parseValue(new Cursor(literal));
            if (!(parsed instanceof List<?> list)) {
                // Should never happen for a bracketed value, but keep a guard.
                stack.push(parsed);
            } else {
                stack.push(deepUnmodifiable(list));
            }
        } else {
            // Otherwise, push the string literal as-is.
            stack.push(literal);
        }
        return pc + 1;
    }

    /** Deeply wrap lists as unmodifiable; leave scalars intact. */
    private static Object deepUnmodifiableObject(Object v) {
        if (v instanceof List<?> l) {
            return deepUnmodifiable(l);
        }
        return v;
    }

    private static List<?> deepUnmodifiable(List<?> l) {
        List<Object> out = new ArrayList<>(l.size());
        for (Object v : l) out.add(deepUnmodifiableObject(v));
        return Collections.unmodifiableList(out);
    }

    // ======== Recursive-descent parser for array literals ========

    private static Object parseValue(Cursor c) {
        skipWs(c);
        if (c.end()) return "";
        char ch = c.ch();
        if (ch == '[') return parseArray(c);
        if (ch == '\"') return parseQuoted(c);
        return parseAtom(c);
    }

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

    private static String parseQuoted(Cursor c) {
        // assumes current char is '"'
        expect(c, '\"');
        StringBuilder sb = new StringBuilder();
        while (!c.end()) {
            char ch = c.ch();
            c.i++;
            if (ch == '\\') { // escape
                if (c.end()) break;
                char nxt = c.ch();
                c.i++;
                switch (nxt) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case '\"' -> sb.append('\"');
                    case '\\' -> sb.append('\\');
                    default -> sb.append(nxt);
                }
            } else if (ch == '\"') {
                // end quote
                return sb.toString();
            } else {
                sb.append(ch);
            }
        }
        // Unclosed string: return what we have
        return sb.toString();
    }

    private static Object parseAtom(Cursor c) {
        int start = c.i;
        while (!c.end()) {
            char ch = c.ch();
            if (ch == ',' || ch == ']' ) break;
            if (Character.isWhitespace(ch)) break;
            c.i++;
        }
        String token = c.s.substring(start, c.i).trim();
        if (token.isEmpty()) return "";
        // booleans
        if ("true".equalsIgnoreCase(token)) return 1;
        if ("false".equalsIgnoreCase(token)) return 0;
        // number (int or double)
        try {
            if (token.contains(".") || token.contains("e") || token.contains("E")) {
                return Double.parseDouble(token);
            } else {
                return Integer.parseInt(token);
            }
        } catch (NumberFormatException ex) {
            // fallback: raw string
            return token;
        }
    }

    private static void skipWs(Cursor c) {
        while (!c.end() && Character.isWhitespace(c.ch())) c.i++;
    }

    private static boolean peek(Cursor c, char ch) {
        return !c.end() && c.ch() == ch;
    }

    private static void expect(Cursor c, char ch) {
        if (c.end() || c.ch() != ch)
            throw new IllegalArgumentException("R_PUSH array literal parse error: expected '" + ch + "' at position " + c.i);
        c.i++; // consume
        skipWs(c);
    }
}
