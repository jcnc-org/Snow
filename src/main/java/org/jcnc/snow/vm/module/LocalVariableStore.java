package org.jcnc.snow.vm.module;

import org.jcnc.snow.common.SnowConfig;
import org.jcnc.snow.vm.gui.LocalVariableStoreSwing;
import org.jcnc.snow.vm.utils.LoggingUtils;

import java.util.ArrayList;

import static org.jcnc.snow.vm.utils.VMUtils.isNativeImage;

/**
 * The {@code LocalVariableStore} represents a simple, dynamically-sized
 * local-variable table (<em>frame locals</em>) for the virtual machine (VM).
 *
 * <p>This class supports random access for storing and retrieving variables
 * via {@link #setVariable(int, Object)} and {@link #getVariable(int)}.
 * It can also <strong>compact</strong> itself by trimming trailing {@code null}
 * slots after execution, for cleaner debug output.
 *
 * <p>Internally, it uses an {@link ArrayList} to store variables, automatically
 * expanding as needed to support random index writes. This structure is designed
 * for VM stack frame management and debug inspection.
 */
public class LocalVariableStore {

    /**
     * The backing list storing the local variables.
     */
    private final ArrayList<Object> localVariables;

    /* ---------- Construction ---------- */

    /**
     * Constructs a new {@code LocalVariableStore} with the given initial capacity.
     *
     * @param initialCapacity the initial capacity for the local variable list
     */
    public LocalVariableStore(int initialCapacity) {
        this.localVariables = new ArrayList<>(initialCapacity);
        handleMode();
    }

    /**
     * Constructs a new {@code LocalVariableStore} with default capacity.
     */
    public LocalVariableStore() {
        this.localVariables = new ArrayList<>();
        handleMode();
    }

    /* ---------- Public API ---------- */

    /**
     * Sets the value at the specified index, expanding the list if necessary.
     * <p>If the list is smaller than {@code index + 1}, it will be padded
     * with {@code null} until the required capacity is reached.
     *
     * @param index the index to set (0-based)
     * @param value the value to store at the specified index
     * @throws IndexOutOfBoundsException if {@code index} is negative
     */
    public void setVariable(int index, Object value) {
        ensureCapacity(index + 1);
        localVariables.set(index, value);
    }

    /**
     * Stores a value at the specified index (alias for {@link #setVariable}).
     * <p>This method is provided for VM instruction decoder compatibility.
     *
     * @param index the index to set
     * @param value the value to store
     */
    public void store(int index, Object value) {
        setVariable(index, value);
    }

    /**
     * Loads the value from the specified index (alias for {@link #getVariable}).
     * <p>This method is provided for VM instruction decoder compatibility.
     *
     * @param index the index to retrieve
     * @return the value at the given index
     */
    public Object load(int index) {
        return getVariable(index);
    }

    /**
     * Returns the value at the specified index.
     * <p>If the list is not large enough, it is automatically expanded,
     * filling new slots with {@code null} values.
     *
     * @param index the index to retrieve (0-based)
     * @return the value at the specified index, or {@code null} if not set
     * @throws IndexOutOfBoundsException if {@code index} is negative
     */
    public Object getVariable(int index) {
        // Automatic expansion to avoid LOAD out-of-bounds exception.
        if (index < 0)
            throw new IndexOutOfBoundsException("Negative LV index: " + index);
        ensureCapacity(index + 1);
        return localVariables.get(index);
    }

    /**
     * Returns the backing {@link ArrayList} storing the local variables.
     * <p>Direct modification is not recommended. Prefer read-only usage.
     *
     * @return the internal list of local variables
     */
    public ArrayList<Object> getLocalVariables() {
        return localVariables;
    }

    /**
     * Prints the contents of each slot in the local variable table to the logger.
     * <p>If the table is empty, a corresponding message is printed instead.
     */
    public void printLv() {
        if (localVariables.isEmpty()) {
            LoggingUtils.logInfo("Local variable table is empty", "");
            return;
        }
        LoggingUtils.logInfo("\n### VM Local Variable Table:", "");
        for (int i = 0; i < localVariables.size(); i++) {
            LoggingUtils.logInfo("",
                    String.format("%d: %s", i, localVariables.get(i)));
        }
    }

    /* ---------- Internal Helpers ---------- */

    /**
     * Clears all variables in the table.
     * <p>This method is typically called when a stack frame is popped.
     */
    public void clearVariables() {
        localVariables.clear();
    }

    /**
     * Compacts the table by removing only trailing {@code null} slots.
     * <p>This should be called after program termination, for cleaner debug output.
     * Does not affect non-null slots or internal indices during execution.
     */
    public void compact() {
        // Only delete the "tail" null values, not filter the entire table.
        int i = localVariables.size() - 1;
        while (i >= 0 && localVariables.get(i) == null) {
            localVariables.remove(i);
            i--;
        }
    }

    /**
     * Ensures that the backing list has at least the specified minimum capacity.
     * <p>New slots are filled with {@code null} values if the list needs to grow.
     *
     * @param minCapacity the minimum capacity required
     */
    private void ensureCapacity(int minCapacity) {
        while (localVariables.size() < minCapacity) {
            localVariables.add(null);
        }
    }

    /**
     * Handles debug mode hooks. If debug mode is enabled and not running inside a
     * GraalVM native-image, this method will open a Swing-based variable inspector
     * window for debugging purposes.
     * <p>In native-image environments, this window is not displayed.
     */
    private void handleMode() {
        if (SnowConfig.isDebug()) {
            if (isNativeImage()) return;
            LocalVariableStoreSwing.display(this, "Local Variable Table");
        }
    }

}
