package org.jcnc.snow.vm.module;

import java.util.HashMap;
import java.util.Map;

/**
 * The {@code Instance} class represents a runtime object instance
 * in the virtual machine.
 * <p>
 * Each instance maintains:
 * <ul>
 *     <li>A reference to its {@link VirtualTable} (vtable), which provides
 *     dynamic method dispatch and runtime method lookup.</li>
 *     <li>A map of field values, where each field is identified by name
 *     and can hold an arbitrary object reference.</li>
 * </ul>
 *
 * <p>Key responsibilities:</p>
 * <ul>
 *     <li>Encapsulate the runtime state of an object.</li>
 *     <li>Enable field storage and retrieval through {@link #setField(String, Object)}
 *     and {@link #getField(String)}.</li>
 *     <li>Provide access to the instance’s virtual table for method resolution.</li>
 * </ul>
 */
public final class Instance {

    /** The virtual table associated with this instance (immutable). */
    private final VirtualTable vtable;

    /** A mapping of field names to their runtime values. */
    private final Map<String, Object> fields = new HashMap<>();

    /**
     * Constructs a new {@code Instance} with the specified virtual table.
     *
     * @param vtable The virtual table that defines the method dispatch behavior
     *               for this instance. Must not be {@code null}.
     */
    public Instance(VirtualTable vtable) {
        this.vtable = vtable;
    }

    /* ---------- Virtual Table ---------- */

    /**
     * Returns the virtual table associated with this instance.
     * <p>
     * The virtual table is used for runtime method resolution
     * (dynamic dispatch).
     * </p>
     *
     * @return The {@link VirtualTable} of this instance.
     */
    public VirtualTable vtable() {
        return vtable;
    }

    /* ---------- Fields ---------- */

    /**
     * Sets the value of a field in this instance.
     * <p>
     * If the field does not already exist, it is created.
     * If the field exists, its value is overwritten.
     * </p>
     *
     * @param name  The name of the field to set. Must not be {@code null}.
     * @param value The value to assign to the field. Can be {@code null}.
     */
    public void setField(String name, Object value) {
        fields.put(name, value);
    }

    /**
     * Retrieves the value of a field in this instance.
     * <p>
     * The value is cast to the expected type at runtime.
     * If the field is not present, this method returns {@code null}.
     * </p>
     *
     * @param name The name of the field to retrieve. Must not be {@code null}.
     * @param <T>  The expected type of the field value.
     * @return The field value cast to type {@code T}, or {@code null} if absent.
     * @throws ClassCastException If the stored value cannot be cast to {@code T}.
     */
    @SuppressWarnings("unchecked")
    public <T> T getField(String name) {
        return (T) fields.get(name);
    }
}
