package org.jcnc.snow.vm.utils;

import org.graalvm.nativeimage.ImageInfo;
import org.jcnc.snow.vm.engine.VirtualMachineEngine;

/**
 * Utility class for virtual machine operations.
 * <p>
 * This class provides helper methods for managing and inspecting the state
 * of a {@link VirtualMachineEngine}. It is intended for debugging and
 * monitoring purposes.
 * </p>
 * <p>
 * The {@code VMUtils} class is not meant to be instantiated, as all methods
 * are static.
 * </p>
 */
public class VMUtils {
    /**
     * Default constructor for creating an instance of VMUtils.
     * This constructor is empty as no specific initialization is required.
     */
    public VMUtils() {
        // Empty constructor
    }

    /**
     * Prints the current state of the virtual machine, including the stack and local variables.
     * This method provides a snapshot of the virtual machine's current execution context,
     * useful for debugging or inspection purposes.
     *
     * @param vm The virtual machine instance whose state will be printed.
     *           The vm parameter must not be null.
     * @throws IllegalArgumentException If the provided vm is null.
     */
    public static void printVMState(VirtualMachineEngine vm) {
        if (vm == null) {
            throw new IllegalArgumentException("VirtualMachineEngine instance cannot be null.");
        }
        vm.printStack();
        vm.printLocalVariables();
    }

    /**
     * Detects if the current runtime environment is a GraalVM native-image.
     * <p>
     * Uses GraalVM's {@code org.graalvm.nativeimage.ImageInfo.inImageCode()} API to determine
     * if the application is running as a native executable. If the class is not present
     * (for example, in a standard JVM), returns {@code false}.
     *
     * @return {@code true} if running inside a GraalVM native-image, otherwise {@code false}
     */
    public static boolean isNativeImage() {
        try {
            return ImageInfo.inImageCode();
        } catch (Throwable t) {
            return false;
        }
    }
}
