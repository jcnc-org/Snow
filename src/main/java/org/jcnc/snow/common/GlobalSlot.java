package org.jcnc.snow.common;

/**
 * Helper utilities for global slot numbering.
 *
 * <p>
 * Global variables are assigned a large, reserved slot range so they never
 * conflict with per-function local slots. On the VM side, all accesses to
 * this global slot range are redirected into a shared global storage area.
 * </p>
 */
public final class GlobalSlot {

    /**
     * The starting slot index reserved for global variables.
     */
    public static final int BASE = 1_000_000;

    private GlobalSlot() {
        // Utility class; do not instantiate.
    }

    /**
     * Returns true if the given slot id lies within the global slot range.
     *
     * @param slot the absolute slot id
     * @return whether this slot belongs to the global region
     */
    public static boolean isGlobal(int slot) {
        return slot >= BASE;
    }

    /**
     * Converts an absolute slot id into a zero-based index in the global store.
     *
     * @param slot the absolute slot id
     * @return the corresponding index in the global storage array
     */
    public static int toIndex(int slot) {
        return slot - BASE;
    }

    /**
     * Converts a zero-based global store index into an absolute slot id.
     *
     * @param index the index in the global storage array
     * @return the corresponding absolute slot id
     */
    public static int fromIndex(int index) {
        return BASE + index;
    }
}