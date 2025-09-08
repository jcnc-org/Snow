package org.jcnc.snow.vm.module;

import java.util.HashMap;
import java.util.Map;

/**
 * The {@code VirtualTable} class represents a runtime virtual function table (vtable)
 * used by the virtual machine to support dynamic method dispatch.
 * <p>
 * A vtable is a mapping from method signatures to their corresponding
 * entry addresses in the VM code segment. Each class or instance can have
 * its own vtable that determines which concrete method implementations
 * are invoked at runtime.
 * </p>
 *
 * <p>Key details:</p>
 * <ul>
 *     <li><b>Key:</b> A method signature, such as {@code "Person::getName"}.</li>
 *     <li><b>Value:</b> The entry point address of the method implementation
 *         within the VM code segment.</li>
 * </ul>
 */
public final class VirtualTable {

    /**
     * Mapping from method signature to its entry address in the VM code segment.
     */
    private final Map<String, Integer> table = new HashMap<>();

    /**
     * Registers a new method implementation in the virtual table, or overrides
     * an existing entry if the method signature is already present.
     *
     * @param methodSig The method signature (e.g., {@code "Person::getName"}).
     * @param addr      The entry address of the method implementation in the VM code segment.
     */
    public void register(String methodSig, int addr) {
        table.put(methodSig, addr);
    }

    /**
     * Looks up the entry address for the given method signature.
     * <p>
     * If the method is not found in the virtual table, an {@link IllegalStateException}
     * is thrown to aid debugging.
     * </p>
     *
     * @param methodSig The method signature to look up.
     * @return The entry address of the corresponding method implementation.
     * @throws IllegalStateException If no mapping exists for the given method signature.
     */
    public int lookup(String methodSig) {
        Integer addr = table.get(methodSig);
        if (addr == null)
            throw new IllegalStateException("VTable missing entry: " + methodSig);
        return addr;
    }

    /**
     * Returns a read-only snapshot of the current virtual table.
     * <p>
     * This is primarily intended for debugging or inspection purposes,
     * allowing external code to see the current state of the vtable
     * without modifying it.
     * </p>
     *
     * @return An unmodifiable copy of the method signature to entry address mappings.
     */
    public Map<String, Integer> snapshot() {
        return Map.copyOf(table);
    }
}
