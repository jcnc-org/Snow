package org.jcnc.snow.vm.engine;

/**
 * The VMMode enum defines the different operational modes of the virtual machine.
 * <p>This class is used to distinguish the behavior of the virtual machine in different states, with each mode corresponding to a different operational logic.</p>
 */
public enum VMMode {
    /**
     * Run Mode: The virtual machine executes instructions in the normal execution flow.
     * <p>In this mode, the virtual machine processes instructions and performs related calculations.</p>
     */
    RUN,

    /**
     * Debug Mode: The virtual machine outputs debug information during execution.
     * <p>This mode is used for debugging the virtual machine, allowing developers to view detailed information such as the execution state, local variables, and more.</p>
     */
    DEBUG,
}
