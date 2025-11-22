package org.jcnc.snow.vm.utils;

import org.jcnc.snow.vm.module.OperandStack;

/**
 * Numeric conversion helpers shared by VM commands.
 * <p>
 * Several parts of the compiler still emit int32 opcodes even when the backing
 * slot currently holds a {@link Short} or {@link Byte}.  Instead of crashing
 * with {@link ClassCastException}, we widen those values lazily when the
 * instruction actually consumes them.
 * </p>
 */
public final class NumberUtils {

    private NumberUtils() {
    }

    /**
     * Pops a value from the operand stack and widens it to {@code int}.
     *
     * @param stack  operand stack
     * @param opcode opcode name (for diagnostics)
     * @return widened int value
     */
    public static int popInt(OperandStack stack, String opcode) {
        Object raw = stack.pop();
        return toInt(raw, opcode);
    }

    /**
     * Converts arbitrary numeric payloads (Integer/Short/Byte/Long/Boolean, etc.)
     * into an {@code int}.
     *
     * @param raw    raw value
     * @param opcode opcode name (for diagnostics)
     * @return widened int value
     */
    public static int toInt(Object raw, String opcode) {
        if (raw instanceof Integer i) {
            return i;
        }
        if (raw instanceof Number n) {
            return n.intValue();
        }
        if (raw instanceof Boolean b) {
            return b ? 1 : 0;
        }
        if (raw == null) {
            throw new IllegalStateException(opcode + " encountered null operand");
        }
        throw new IllegalStateException(opcode + " expects numeric operand but got "
                + raw.getClass().getSimpleName());
    }

    /**
     * Pops a value from the operand stack and widens it to {@code long}.
     *
     * @param stack  operand stack
     * @param opcode opcode name (for diagnostics)
     * @return widened long value
     */
    public static long popLong(OperandStack stack, String opcode) {
        Object raw = stack.pop();
        return toLong(raw, opcode);
    }

    /**
     * Converts arbitrary numeric payloads (Long/Integer/Short/Byte/Boolean, etc.)
     * into a {@code long}.
     *
     * @param raw    raw value
     * @param opcode opcode name (for diagnostics)
     * @return widened long value
     */
    public static long toLong(Object raw, String opcode) {
        if (raw instanceof Long l) {
            return l;
        }
        if (raw instanceof Number n) {
            return n.longValue();
        }
        if (raw instanceof Boolean b) {
            return b ? 1L : 0L;
        }
        if (raw == null) {
            throw new IllegalStateException(opcode + " encountered null operand");
        }
        throw new IllegalStateException(opcode + " expects numeric operand but got "
                + raw.getClass().getSimpleName());
    }

    public static short popShort(OperandStack stack, String opcode) {
        Object raw = stack.pop();
        return toShort(raw, opcode);
    }

    public static short toShort(Object raw, String opcode) {
        if (raw instanceof Short s) {
            return s;
        }
        if (raw instanceof Number n) {
            return (short) n.intValue();
        }
        if (raw instanceof Boolean b) {
            return (short) (b ? 1 : 0);
        }
        if (raw == null) {
            throw new IllegalStateException(opcode + " encountered null operand");
        }
        throw new IllegalStateException(opcode + " expects numeric operand but got "
                + raw.getClass().getSimpleName());
    }

    /**
     * Pops a value from the operand stack and widens it to {@code float}.
     *
     * @param stack  operand stack
     * @param opcode opcode name (for diagnostics)
     * @return widened float value
     */
    public static float popFloat(OperandStack stack, String opcode) {
        Object raw = stack.pop();
        return toFloat(raw, opcode);
    }

    /**
     * Converts arbitrary numeric payloads (Float/Double/Number/Boolean, etc.)
     * into a {@code float}.
     */
    public static float toFloat(Object raw, String opcode) {
        if (raw instanceof Float f) {
            return f;
        }
        if (raw instanceof Number n) {
            return n.floatValue();
        }
        if (raw instanceof Boolean b) {
            return b ? 1.0f : 0.0f;
        }
        if (raw == null) {
            throw new IllegalStateException(opcode + " encountered null operand");
        }
        throw new IllegalStateException(opcode + " expects numeric operand but got "
                + raw.getClass().getSimpleName());
    }

    /**
     * Pops a value from the operand stack and widens it to {@code double}.
     *
     * @param stack  operand stack
     * @param opcode opcode name (for diagnostics)
     * @return widened double value
     */
    public static double popDouble(OperandStack stack, String opcode) {
        Object raw = stack.pop();
        return toDouble(raw, opcode);
    }

    /**
     * Converts arbitrary numeric payloads (Double/Float/Number/Boolean, etc.)
     * into a {@code double}.
     */
    public static double toDouble(Object raw, String opcode) {
        if (raw instanceof Double d) {
            return d;
        }
        if (raw instanceof Number n) {
            return n.doubleValue();
        }
        if (raw instanceof Boolean b) {
            return b ? 1.0d : 0.0d;
        }
        if (raw == null) {
            throw new IllegalStateException(opcode + " encountered null operand");
        }
        throw new IllegalStateException(opcode + " expects numeric operand but got "
                + raw.getClass().getSimpleName());
    }
}
