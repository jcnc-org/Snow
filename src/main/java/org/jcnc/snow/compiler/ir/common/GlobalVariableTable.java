package org.jcnc.snow.compiler.ir.common;

import org.jcnc.snow.common.GlobalSlot;
import org.jcnc.snow.compiler.ir.value.IRVirtualRegister;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Registry for mutable module-level global variables.
 *
 * <p>
 * Each global variable is assigned:
 * </p>
 * <ul>
 *   <li>a fully qualified name (e.g. {@code module.var})</li>
 *   <li>a shared IR virtual register instance that is reused across all functions</li>
 *   <li>a reserved VM slot id within the global slot range</li>
 * </ul>
 *
 * <p>
 * The shared register ensures that all IR code referring to the same global
 * maps to the same slot id, and the VM will automatically redirect accesses to
 * this slot into the shared global storage area.
 * </p>
 */
public final class GlobalVariableTable {

    /**
     * Ordered registry of all declared global variables.
     *
     * <p>
     * A {@link LinkedHashMap} is used to preserve declaration order so that
     * global slot numbering is deterministic and stable across runs.
     * </p>
     */
    private static final Map<String, GlobalVariable> VARS = new LinkedHashMap<>();

    private GlobalVariableTable() {
        // Utility class; do not instantiate.
    }

    /**
     * Registers a mutable global variable or returns the existing entry if one
     * already exists with the same qualified name.
     *
     * @param qualifiedName the fully qualified name (e.g. {@code main.x})
     * @param type          the declared type of the global
     * @return the registry entry for the global
     */
    public static GlobalVariable register(String qualifiedName, String type) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        return VARS.computeIfAbsent(qualifiedName, name -> {
            // Assign the next available global slot
            int slot = GlobalSlot.fromIndex(VARS.size());
            String simple = extractSimpleName(name);
            IRVirtualRegister reg = new IRVirtualRegister(slot);
            return new GlobalVariable(name, simple, type, reg, slot);
        });
    }

    /**
     * Retrieves a global variable entry by its qualified name, or returns null
     * if not registered.
     */
    public static GlobalVariable get(String qualifiedName) {
        return VARS.get(qualifiedName);
    }

    /**
     * Returns an immutable collection of all registered global variables.
     */
    public static Collection<GlobalVariable> all() {
        return Collections.unmodifiableCollection(VARS.values());
    }

    /**
     * Returns all global variables belonging to the specified module.
     *
     * @param moduleName the module name (e.g. {@code main})
     * @return an immutable collection of all globals under that module
     */
    public static Collection<GlobalVariable> ofModule(String moduleName) {
        if (moduleName == null || moduleName.isBlank()) {
            return Collections.emptyList();
        }
        String prefix = moduleName + ".";
        return VARS.values().stream()
                .filter(g -> g.qualifiedName().startsWith(prefix))
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Returns whether the given virtual register corresponds to one of the
     * shared global registers.
     */
    public static boolean isGlobal(IRVirtualRegister reg) {
        return VARS.values().stream().anyMatch(g -> g.register().equals(reg));
    }

    /**
     * Returns the assigned global slot id for the given register, or {@code -1}
     * if the register is not a global.
     */
    public static int slotOf(IRVirtualRegister reg) {
        for (GlobalVariable v : VARS.values()) {
            if (v.register().equals(reg)) {
                return v.slot();
            }
        }
        return -1;
    }

    /**
     * Extracts the simple name (the part after the last dot) from a qualified name.
     */
    private static String extractSimpleName(String qualifiedName) {
        int dot = qualifiedName.lastIndexOf('.');
        if (dot < 0 || dot + 1 >= qualifiedName.length()) return qualifiedName;
        return qualifiedName.substring(dot + 1);
    }

    /**
     * Immutable view of a registered global variable.
     */
    public record GlobalVariable(
            String qualifiedName,
            String simpleName,
            String type,
            IRVirtualRegister register,
            int slot
    ) {
    }
}