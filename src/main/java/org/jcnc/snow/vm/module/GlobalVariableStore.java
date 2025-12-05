package org.jcnc.snow.vm.module;

import java.util.ArrayList;

/**
 * Shared storage for mutable global variables.
 *
 * <p>
 * This class provides a simple growable backing store for all global
 * variables in the VM. The storage is indexed using the zero-based
 * global index derived from the global slot range
 * (see {@code GlobalSlot}).
 * </p>
 *
 * <p>
 * All methods operate on a single shared list, and the store grows
 * automatically as needed.
 * </p>
 */
public final class GlobalVariableStore {

    private static final ArrayList<Object> globals = new ArrayList<>();

    private GlobalVariableStore() {
        // Utility class; prevent instantiation.
    }

    /**
     * Sets the value of the global variable at the given index.
     * The store automatically expands to fit the index.
     *
     * @param index zero-based global storage index
     * @param value the value to store
     */
    public static void set(int index, Object value) {
        ensureCapacity(index + 1);
        globals.set(index, value);
    }

    /**
     * Retrieves the value of the global variable at the given index.
     * If the index has not been written before, {@code null} is returned.
     * The store automatically expands to fit the index.
     *
     * @param index zero-based global storage index
     * @return the stored value or {@code null} if uninitialized
     */
    public static Object get(int index) {
        ensureCapacity(index + 1);
        return globals.get(index);
    }

    /**
     * Clears all stored global variables.
     * After clearing, the store behaves as if newly initialized.
     */
    public static void clear() {
        globals.clear();
    }

    /**
     * Ensures that the backing list has at least the given capacity.
     * Missing entries are initialized to {@code null}.
     *
     * @param minCapacity the required minimum size
     */
    private static void ensureCapacity(int minCapacity) {
        while (globals.size() < minCapacity) {
            globals.add(null);
        }
    }
}