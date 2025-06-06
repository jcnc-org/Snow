package org.jcnc.snow.vm.engine;


import org.jcnc.snow.vm.utils.LoggingUtils;

import java.util.List;

/**
 * Virtual Machine Command Executor (VMCommandExecutor)
 * <p>
 * This class is responsible for executing a set of instructions for the virtual machine. It calls the `execute` method of
 * {@link VirtualMachineEngine} to execute the provided list of instructions. If any errors occur during execution, the
 * error messages are logged using {@link LoggingUtils}.
 * </p>
 */
public class VMCommandExecutor {
    /**
     * Default constructor for creating an instance of VMCommandExecutor.
     * This constructor is empty as no specific initialization is required.
     */
    public VMCommandExecutor() {
        // Empty constructor
    }

    /**
     * Executes the virtual machine instructions.
     * <p>
     * This method takes a virtual machine instance and a list of instructions, and executes them while controlling the
     * virtual machine's operation. If any exceptions occur during execution, they are caught and logged via {@link LoggingUtils}.
     * </p>
     *
     * @param virtualMachineEngine The virtual machine instance used to execute the instructions.
     * @param instructions         The list of instructions to be executed.
     */
    public static void executeInstructions(VirtualMachineEngine virtualMachineEngine, List<String> instructions) {
        try {
            // Call the virtual machine's execute method to run the instruction list
            virtualMachineEngine.execute(instructions);
        } catch (Exception e) {
            // If an exception occurs, log the error message
            LoggingUtils.logError("Error while executing instructions: " + e.getMessage());
        }
    }
}
