package org.jcnc.snow.vm.module;

import org.jcnc.snow.common.GlobalSlot;
import org.jcnc.snow.common.SnowConfig;
import org.jcnc.snow.vm.gui.LocalVariableStoreSwing;
import org.jcnc.snow.vm.utils.LoggingUtils;

import java.util.ArrayList;

import static org.jcnc.snow.vm.utils.VMUtils.isNativeImage;

/**
 * {@code LocalVariableStore} provides a dynamically-sized local variable table (frame locals)
 * for the virtual machine (VM) stack frame management.
 *
 * <p>
 * This class supports random access storage and retrieval of variables, and allows
 * automatic expansion of the table as needed. It is mainly used for VM stack frame management,
 * debugging, and inspection.
 * </p>
 *
 * <p>
 * Core features include:
 * <ul>
 *     <li>Random access get/set operations by index</li>
 *     <li>Automatic expansion and compaction of storage slots</li>
 *     <li>Integration with VM's global variable store (via GlobalSlot check)</li>
 *     <li>Debugging and inspection utilities for local variables</li>
 * </ul>
 * </p>
 *
 * <p>
 * <strong>Thread Safety:</strong> This class is <b>not</b> thread-safe.
 * </p>
 *
 * @author (your name)
 * @version 1.0
 * @since 2024-12-05
 */
public class LocalVariableStore {

    /**
     * The backing list storing local variable values.
     */
    private final ArrayList<Object> localVariables;

    /* ---------- Construction ---------- */

    /**
     * Constructs a new {@code LocalVariableStore} with the specified initial capacity.
     *
     * @param initialCapacity the initial capacity for the local variable table
     */
    public LocalVariableStore(int initialCapacity) {
        this.localVariables = new ArrayList<>(initialCapacity);
        // handleMode(); // Uncomment for debug-mode Swing window
    }

    /**
     * Constructs a new {@code LocalVariableStore} with the default initial capacity.
     */
    public LocalVariableStore() {
        this.localVariables = new ArrayList<>();
        // handleMode(); // Uncomment for debug-mode Swing window
    }

    /* ---------- Public API ---------- */

    /**
     * Sets the value at the specified index, automatically expanding the table if required.
     * <p>
     * If the index refers to a global slot (determined by {@link GlobalSlot#isGlobal(int)}),
     * the value is set in the global variable store instead.
     * </p>
     *
     * @param index the zero-based index at which to store the value
     * @param value the value to store; may be {@code null}
     * @throws IndexOutOfBoundsException if {@code index} is negative
     */
    public void setVariable(int index, Object value) {
        if (GlobalSlot.isGlobal(index)) {
            GlobalVariableStore.set(GlobalSlot.toIndex(index), value);
            return;
        }
        ensureCapacity(index + 1);
        localVariables.set(index, value);
    }

    /**
     * Stores a value at the specified index (alias for {@link #setVariable}).
     * <p>
     * This method is intended for VM instruction decoder compatibility.
     * </p>
     *
     * @param index the index to store the value at
     * @param value the value to store
     */
    public void store(int index, Object value) {
        setVariable(index, value);
    }

    /**
     * Loads and returns the value at the specified index (alias for {@link #getVariable}).
     * <p>
     * This method is intended for VM instruction decoder compatibility.
     * </p>
     *
     * @param index the index to retrieve the value from
     * @return the value at the specified index, or {@code null} if not set
     */
    public Object load(int index) {
        return getVariable(index);
    }

    /**
     * Returns the value at the specified index.
     * <p>
     * If the index refers to a global slot, retrieves from the global variable store.
     * Otherwise, expands the local variable table as needed.
     * </p>
     *
     * @param index the zero-based index to retrieve the value from
     * @return the value at the specified index, or {@code null} if unset
     * @throws IndexOutOfBoundsException if {@code index} is negative
     */
    public Object getVariable(int index) {
        if (GlobalSlot.isGlobal(index)) {
            return GlobalVariableStore.get(GlobalSlot.toIndex(index));
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("Negative local variable index: " + index);
        }
        ensureCapacity(index + 1);
        return localVariables.get(index);
    }

    /**
     * Returns the backing {@link ArrayList} containing local variable values.
     * <p>
     * <b>Note:</b> Direct modification is not recommended; prefer read-only usage.
     * </p>
     *
     * @return the backing list of local variables
     */
    public ArrayList<Object> getLocalVariables() {
        return localVariables;
    }

    /**
     * Prints the contents of each slot in the local variable table using the logger.
     * <p>
     * If the table is empty, a message is logged instead.
     * </p>
     */
    public void printLv() {
        if (localVariables.isEmpty()) {
            LoggingUtils.logInfo("Local variable table is empty", "");
            return;
        }
        LoggingUtils.logInfo("\n### VM Local Variable Table:", "");
        for (int i = 0; i < localVariables.size(); i++) {
            LoggingUtils.logInfo("", String.format("%d: %s", i, localVariables.get(i)));
        }
    }

    /* ---------- Internal Helpers ---------- */

    /**
     * Clears all variables in the local variable table.
     * <p>
     * Typically invoked when a stack frame is popped or reset.
     * </p>
     */
    public void clearVariables() {
        localVariables.clear();
    }

    /**
     * Compacts the local variable table by removing only trailing {@code null} slots.
     * <p>
     * Intended for use after program termination, to provide cleaner debug output.
     * Does not affect non-{@code null} slots or indices in use during execution.
     * </p>
     */
    public void compact() {
        int i = localVariables.size() - 1;
        while (i >= 0 && localVariables.get(i) == null) {
            localVariables.remove(i);
            i--;
        }
    }

    /**
     * Ensures that the backing list has at least the specified minimum capacity.
     * <p>
     * If the list needs to grow, new slots are filled with {@code null}.
     * </p>
     *
     * @param minCapacity the minimum required capacity
     */
    private void ensureCapacity(int minCapacity) {
        while (localVariables.size() < minCapacity) {
            localVariables.add(null);
        }
    }

    /**
     * Handles debug mode integration. If debug mode is enabled and not running in
     * a GraalVM native image, this method displays a Swing-based variable inspector
     * for debugging purposes.
     * <p>
     * In native-image environments, this method does nothing.
     * </p>
     */
    private void handleMode() {
        if (SnowConfig.isDebug()) {
            if (isNativeImage()) return;
            LocalVariableStoreSwing.display(this, "Local Variable Table");
        }
    }

}