package org.jcnc.snow.vm.utils;

import org.jcnc.snow.vm.engine.VirtualMachineEngine;

/**
 * Utility class for logging the state of a virtual machine.
 * <p>
 * The {@code VMStateLogger} class provides methods to log and display
 * the current execution state of a {@link VirtualMachineEngine}, including
 * its stack, local variables, and other relevant details.
 * </p>
 * <p>
 * This class serves as a wrapper around the {@link VMUtils#printVMState(VirtualMachineEngine)}
 * method, simplifying the logging process.
 * </p>
 */
public class VMStateLogger {

    /**
     * Default constructor for creating an instance of {@code VMStateLogger}.
     * <p>
     * This constructor is intentionally left empty as no specific initialization is required.
     * </p>
     */
    public VMStateLogger() {
        // Empty constructor
    }

    /**
     * Prints the current state of the virtual machine.
     * <p>
     * This method logs the current state of the provided {@link VirtualMachineEngine},
     * including the stack contents, local variables, and other relevant execution details.
     * It delegates the actual state printing to the {@link VMUtils#printVMState(VirtualMachineEngine)} method.
     * </p>
     *
     * @param virtualMachineEngine The virtual machine instance whose state will be printed.
     *                             Must not be {@code null}.
     * @throws IllegalArgumentException If the provided {@code virtualMachineEngine} is {@code null}.
     */
    public static void printVMState(VirtualMachineEngine virtualMachineEngine) {
        if (virtualMachineEngine == null) {
            throw new IllegalArgumentException("VirtualMachineEngine instance cannot be null.");
        }

        VMUtils.printVMState(virtualMachineEngine);
    }
}
