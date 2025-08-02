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
 * <b>LoadConstGenerator - Generates VM instructions from IR {@code LoadConstInstruction}</b>
 *
 * <p>
 * This class is responsible for converting IR-level {@link LoadConstInstruction} into corresponding VM instructions.
 * If the constant is a {@code String}, it will also be registered in the
 * {@link CallGenerator} string constant pool to support syscall downgrade scenarios.
 * </p>
 *
 * <p>
 * Fix: When the constant is an array (List), type information is preserved in R_PUSH payload:
 * <ul>
 *   <li>Float is output with <code>f</code> suffix (e.g., 0.1f);</li>
 *   <li>Long is output with <code>L</code> suffix (e.g., 123L);</li>
 *   <li>Double/Integer are output in their default format (e.g., 1.0, 42);</li>
 *   <li>Supports recursive serialization of nested arrays.</li>
 * </ul>
 * This prevents float values from being misinterpreted as double on the VM side,
 * and avoids Double→Float cast exceptions in later F_STORE operations.
 * </p>
 */
public class LoadConstGenerator implements InstructionGenerator<LoadConstInstruction> {

    /**
     * Formats a constant value as a string for use as a VM payload.
     * Lists are recursively serialized, and Float/Long types include suffixes to preserve type information.
     *
     * @param v The constant value to format.
     * @return The formatted string for use in VM code.
     */
    private static String formatConst(Object v) {
        return formatConst(v, false);
    }

    /**
     * Internal helper for recursively formatting constant values (including nested arrays)
     * with appropriate type suffixes for array payloads.
     *
     * @param v           The constant value to format.
     * @param insideArray True if currently formatting inside an array context; affects whether type suffixes are applied.
     * @return The formatted string for use in VM code.
     */
    private static String formatConst(Object v, boolean insideArray) {
        if (v instanceof List<?> list) {
            // Recursively process each element in the list
            return "[" + list.stream()
                    .map(x -> formatConst(x, true))
                    .collect(Collectors.joining(", ")) + "]";
        }
        if (v instanceof String s) {
            return s;
        }
        if (v instanceof Float f) {
            // Always keep .0 for integer values
            float fv = f;
            String s = (fv == (long) fv) ? String.format("%.1f", fv) : f.toString();
            return insideArray ? (s + "f") : s;
        }
        if (v instanceof Long l) {
            return insideArray ? (l + "L") : l.toString();
        }
        if (v instanceof Double d) {
            double dv = d;
            // Always keep .0 for integer values
            return (dv == (long) dv) ? String.format("%.1f", dv) : Double.toString(dv);
        }
        if (v instanceof Short s) {
            return insideArray ? (s + "s") : Short.toString(s);
        }
        if (v instanceof Byte b) {
            return insideArray ? (b + "b") : Byte.toString(b);
        }
        if (v instanceof Boolean b) {
            return b ? "1" : "0";
        }
        return String.valueOf(v);
    }


    /**
     * Specifies the type of IR instruction supported by this generator.
     *
     * @return The class object representing {@link LoadConstInstruction}.
     */
    @Override
    public Class<LoadConstInstruction> supportedClass() {
        return LoadConstInstruction.class;
    }

    /**
     * Generates the VM instructions for a given {@link LoadConstInstruction}.
     * <p>
     * This includes formatting the constant value, emitting the corresponding PUSH and STORE instructions,
     * marking the local slot type for later operations, and registering string constants if necessary.
     * </p>
     *
     * @param ins       The {@link LoadConstInstruction} to generate code for.
     * @param out       The {@link VMProgramBuilder} used to collect the generated instructions.
     * @param slotMap   A mapping from {@link IRVirtualRegister} to physical slot indices.
     * @param currentFn The name of the current function.
     */
    @Override
    public void generate(LoadConstInstruction ins,
                         VMProgramBuilder out,
                         Map<IRVirtualRegister, Integer> slotMap,
                         String currentFn) {

        // 1. Get the constant value
        IRConstant constant = (IRConstant) ins.operands().getFirst();
        Object value = constant.value();

        // 2. Generate PUSH instruction (array constants use type-aware formatting)
        String payload = formatConst(value);
        out.emit(OpHelper.pushOpcodeFor(value) + " " + payload);

        // 3. STORE the result to the destination slot
        int slot = slotMap.get(ins.dest());
        out.emit(OpHelper.storeOpcodeFor(value) + " " + slot);

        // 4. Mark the slot's data type for later inference and instruction selection
        char prefix = switch (value) {
            case Integer _ -> 'I';   // Integer
            case Long _ -> 'L';      // Long
            case Short _ -> 'S';     // Short
            case Byte _ -> 'B';      // Byte
            case Double _ -> 'D';    // Double
            case Float _ -> 'F';     // Float
            case Boolean _ -> 'I';   // Boolean handled as Integer (typically lowered to 1/0)
            case String _ -> 'R';    // String constant
            case java.util.List<?> _ -> 'R'; // Reference type (arrays, etc.)
            case null, default -> throw new IllegalStateException("Unknown constant type: "
                    + (value != null ? value.getClass() : null));
        };
        out.setSlotType(slot, prefix);

        // 5. If the constant is a string, register it for the CallGenerator string pool
        if (value instanceof String s) {
            CallGenerator.registerStringConst(ins.dest().id(), s);
        }
    }
}
