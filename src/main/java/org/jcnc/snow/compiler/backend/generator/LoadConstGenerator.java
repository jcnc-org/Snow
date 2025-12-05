package org.jcnc.snow.compiler.backend.generator;

import org.jcnc.snow.compiler.backend.builder.VMProgramBuilder;
import org.jcnc.snow.compiler.backend.core.InstructionGenerator;
import org.jcnc.snow.compiler.backend.utils.OpHelper;
import org.jcnc.snow.compiler.ir.instruction.LoadConstInstruction;
import org.jcnc.snow.compiler.ir.value.IRConstant;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <b>LoadConstGenerator</b>
 *
 * <p>
 * This generator converts an IR-level {@link LoadConstInstruction} into corresponding VM instructions.
 * If the constant is a {@code String}, it will also be registered in the
 * {@link CallGenerator} string constant pool for later use.
 * </p>
 *
 * <p>
 * Key implementation notes:
 * <ul>
 *   <li>When the constant is an array (List), type information is preserved in the R_PUSH payload:</li>
 *   <li>Float values get an <code>f</code> suffix (e.g., 0.1f)</li>
 *   <li>Long values get an <code>L</code> suffix (e.g., 123L)</li>
 *   <li>Double and Integer values use their default string format (e.g., 1.0, 42)</li>
 *   <li>Nested arrays are recursively serialized with correct type suffixes.</li>
 * </ul>
 * This prevents type confusion on the VM side (e.g., float being misread as double)
 * and avoids cast exceptions during store operations.
 * </p>
 */
public class LoadConstGenerator implements InstructionGenerator<LoadConstInstruction> {

    /**
     * Formats a constant value for use as a VM instruction payload.
     * For lists, recursively formats each element with type suffixes where appropriate.
     *
     * @param v The constant value.
     * @return The formatted string payload for VM code.
     */
    private static String formatConst(Object v) {
        return formatConst(v, false);
    }

    /**
     * Recursively formats constant values (including nested arrays), preserving
     * type suffixes and escaping strings. Used internally for array/list handling.
     *
     * @param v           The constant value.
     * @param insideArray Whether this value is inside an array context (controls type suffixing).
     * @return The formatted string for VM code.
     */
    private static String formatConst(Object v, boolean insideArray) {
        if (v instanceof List<?> list) {
            // Recursively process each element in the list
            return "[" + list.stream()
                    .map(x -> formatConst(x, true))
                    .collect(Collectors.joining(", ")) + "]";
        }
        if (v instanceof String s) {
            // Escape and wrap the string in double quotes, to avoid line breaks or control chars breaking VM code
            return "\"" + escape(s) + "\"";
        }
        if (v instanceof Float f) {
            float fv = f;
            String s = (fv == (long) fv) ? String.format("%.1f", fv) : f.toString();
            return insideArray ? (s + "f") : s;
        }
        if (v instanceof Long l) {
            return insideArray ? (l + "L") : l.toString();
        }
        if (v instanceof Double d) {
            double dv = d;
            return (dv == (long) dv) ? String.format("%.1f", dv) : Double.toString(dv);
        }
        if (v instanceof Short s) {
            return insideArray ? (s + "s") : Short.toString(s);
        }
        if (v instanceof Byte b) {
            return insideArray ? (b + "b") : Byte.toString(b);
        }
        if (v instanceof Boolean b) {
            return b ? "true" : "false";
        }
        return String.valueOf(v);
    }

    /**
     * Escapes a string for use in VM code: replaces control characters and all non-ASCII characters
     * with their corresponding escape sequences, so the .water file remains single-line and parseable.
     * Supported escapes: \n, \r, \t, \f, \b, \", \', \\, and Unicode escapes like "uXXXX" for non-ASCII.
     *
     * @param s The input string.
     * @return The escaped string.
     */
    private static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); ++i) {
            char ch = s.charAt(i);
            switch (ch) {
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\f' -> sb.append("\\f");
                case '\b' -> sb.append("\\b");
                case '\"' -> sb.append("\\\"");
                case '\'' -> sb.append("\\'");
                case '\\' -> sb.append("\\\\");
                default -> {
                    if (ch < 0x20 || ch > 0x7E) {
                        sb.append(String.format("\\u%04X", (int) ch));
                    } else {
                        sb.append(ch);
                    }
                }
            }
        }
        return sb.toString();
    }

    @Override
    public Class<LoadConstInstruction> supportedClass() {
        return LoadConstInstruction.class;
    }

    /**
     * Generates VM code for a LoadConstInstruction.
     * Produces PUSH and STORE instructions, sets the slot type,
     * and registers string constants if necessary.
     *
     * @param ins       The IR instruction to generate.
     * @param out       The output program builder.
     * @param slotMap   The mapping from IR virtual register to physical slot.
     * @param currentFn The current function name.
     */
    @Override
    public void generate(LoadConstInstruction ins,
                         VMProgramBuilder out,
                         Map<IRVirtualRegister, Integer> slotMap,
                         String currentFn) {

        // 1. Retrieve the constant value from the instruction
        IRConstant constant = (IRConstant) ins.operands().getFirst();
        Object value = constant.value();

        // 2. Format and emit the PUSH instruction (arrays will use type-aware formatting)
        String payload = formatConst(value);
        out.emit(OpHelper.pushOpcodeFor(value) + " " + payload);

        // 3. Emit STORE to the destination slot
        int slot = slotMap.get(ins.dest());
        out.emit(OpHelper.storeOpcodeFor(value) + " " + slot);

        // 4. Mark the slot's data type for later use (type inference, instruction selection, etc.)
        char prefix = switch (value) {
            case Integer i -> 'I';   // Integer
            case Long l -> 'L';      // Long
            case Short s -> 'S';     // Short
            case Byte b -> 'B';      // Byte
            case Double d -> 'D';    // Double
            case Float f -> 'F';     // Float
            case Boolean b -> 'I';   // Booleans are treated as integers (1/0)
            case String string -> 'R';    // Reference type for strings
            case List<?> list -> 'R'; // Reference type for arrays/lists
            case null, default -> throw new IllegalStateException("Unknown constant type: "
                    + (value != null ? value.getClass() : null));
        };
        out.setSlotType(slot, prefix);

        // 5. Register the string constant for the string constant pool if needed
        if (value instanceof String s) {
            CallGenerator.registerStringConst(ins.dest().id(), s);
        }
    }
}
